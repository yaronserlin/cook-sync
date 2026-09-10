package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.i18n.LocaleContextHolder;

import com.cooksync_server.entities.ContentTranslation;
import com.cooksync_server.entities.TranslationMemory;
import com.cooksync_server.repositories.ContentTranslationRepository;
import com.cooksync_server.repositories.TranslationMemoryRepository;
import com.cooksync_server.translation.TranslationProvider;
import com.cooksync_server.translation.TranslationProvider.TranslationResult;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Unit test suite for {@link TranslationService}, covering every branch of {@link TranslationService#resolve}:
 * the locale-match short-circuit, the entity cache hit/miss paths, the shared
 * {@link TranslationMemory} hit/miss paths, and the provider's success/partial/failure outcomes.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock
    private ContentTranslationRepository translationRepository;
    @Mock
    private TranslationMemoryRepository translationMemoryRepository;
    @Mock
    private TranslationProvider provider;
    @Mock
    private TranslationCacheWriter cacheWriter;

    private SimpleMeterRegistry meterRegistry;
    private TranslationService translationService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        translationService = new TranslationService(
                translationRepository, translationMemoryRepository, provider, cacheWriter, meterRegistry);
        LocaleContextHolder.setLocale(Locale.forLanguageTag("he"));
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void resolve_returnsOriginalUntouched_whenRequestLocaleMatchesSourceLocale() {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("en"));

        TranslationService.TranslatedText result = translationService.resolve(
                ContentTranslation.EntityType.RECIPE_TITLE, "recipe-1", "Chicken Soup", "en");

        assertEquals("Chicken Soup", result.value());
        assertFalse(result.isMachineTranslated());
        verifyNoInteractions(translationRepository, translationMemoryRepository, provider, cacheWriter);
    }

    @Test
    void resolve_returnsOriginalUntouched_forNullOriginal() {
        TranslationService.TranslatedText result = translationService.resolve(
                ContentTranslation.EntityType.RECIPE_TITLE, "recipe-1", null, "en");

        assertNull(result.value());
        assertFalse(result.isMachineTranslated());
    }

    @Test
    void resolve_returnsEntityCacheHit_flaggingMachineSource() {
        when(translationRepository.findByEntityTypeAndEntityIdAndLocale(
                ContentTranslation.EntityType.RECIPE_TITLE, "recipe-1", "he"))
                .thenReturn(Optional.of(ContentTranslation.builder()
                        .value("שקשוקה").source(ContentTranslation.Source.MACHINE).build()));

        TranslationService.TranslatedText result = translationService.resolve(
                ContentTranslation.EntityType.RECIPE_TITLE, "recipe-1", "Shakshuka", "en");

        assertEquals("שקשוקה", result.value());
        assertTrue(result.isMachineTranslated());
        verifyNoInteractions(provider, translationMemoryRepository);
    }

    @Test
    void resolve_returnsEntityCacheHit_notFlaggingHumanSource() {
        when(translationRepository.findByEntityTypeAndEntityIdAndLocale(
                ContentTranslation.EntityType.RECIPE_TITLE, "recipe-1", "he"))
                .thenReturn(Optional.of(ContentTranslation.builder()
                        .value("שקשוקה").source(ContentTranslation.Source.HUMAN).build()));

        TranslationService.TranslatedText result = translationService.resolve(
                ContentTranslation.EntityType.RECIPE_TITLE, "recipe-1", "Shakshuka", "en");

        assertFalse(result.isMachineTranslated());
    }

    @Test
    void resolve_onEntityCacheMiss_returnsSharedMemoryHit_andCopiesItIntoTheEntityCache() {
        when(translationRepository.findByEntityTypeAndEntityIdAndLocale(any(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(translationMemoryRepository.findByTextHashAndLocale(anyString(), eq("he")))
                .thenReturn(Optional.of(TranslationMemory.builder()
                        .value("קמח").source(ContentTranslation.Source.MACHINE).updatedAt(LocalDateTime.now()).build()));

        TranslationService.TranslatedText result = translationService.resolve(
                ContentTranslation.EntityType.INGREDIENT_NAME, "ing-1", "Flour", "en");

        assertEquals("קמח", result.value());
        assertTrue(result.isMachineTranslated());
        verify(cacheWriter).copyToEntityCache(
                ContentTranslation.EntityType.INGREDIENT_NAME, "ing-1", "he", "קמח", ContentTranslation.Source.MACHINE);
        verifyNoInteractions(provider);
    }

    @Test
    void resolve_onDoubleMiss_callsProvider_andCachesACompleteResult() {
        when(translationRepository.findByEntityTypeAndEntityIdAndLocale(any(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(translationMemoryRepository.findByTextHashAndLocale(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(provider.translate("Flour", "he")).thenReturn(Optional.of(new TranslationResult("קמח", true)));

        TranslationService.TranslatedText result = translationService.resolve(
                ContentTranslation.EntityType.INGREDIENT_NAME, "ing-1", "Flour", "en");

        assertEquals("קמח", result.value());
        assertTrue(result.isMachineTranslated());
        verify(cacheWriter).save(any(ContentTranslation.class), anyString());
    }

    @Test
    void resolve_onDoubleMiss_withPartialProviderResult_returnsItButDoesNotCacheIt() {
        when(translationRepository.findByEntityTypeAndEntityIdAndLocale(any(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(translationMemoryRepository.findByTextHashAndLocale(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(provider.translate("Long instructions", "he"))
                .thenReturn(Optional.of(new TranslationResult("partial תרגום Long instructions", false)));

        TranslationService.TranslatedText result = translationService.resolve(
                ContentTranslation.EntityType.INSTRUCTION_TEXT, "inst-1", "Long instructions", "en");

        assertEquals("partial תרגום Long instructions", result.value());
        assertTrue(result.isMachineTranslated());
        verify(cacheWriter, never()).save(any(ContentTranslation.class), anyString());
    }

    @Test
    void resolve_onDoubleMiss_withProviderFailure_fallsBackToOriginal() {
        when(translationRepository.findByEntityTypeAndEntityIdAndLocale(any(), anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(translationMemoryRepository.findByTextHashAndLocale(anyString(), anyString()))
                .thenReturn(Optional.empty());
        when(provider.translate("Flour", "he")).thenReturn(Optional.empty());

        TranslationService.TranslatedText result = translationService.resolve(
                ContentTranslation.EntityType.INGREDIENT_NAME, "ing-1", "Flour", "en");

        assertEquals("Flour", result.value());
        assertFalse(result.isMachineTranslated());
        verify(cacheWriter, never()).save(any(ContentTranslation.class), anyString());
    }

    @Test
    void resolve_treatsLegacyIwLocale_asHebrewSourceMatch() {
        LocaleContextHolder.setLocale(new Locale("iw"));

        TranslationService.TranslatedText result = translationService.resolve(
                ContentTranslation.EntityType.RECIPE_TITLE, "recipe-1", "שקשוקה", "he");

        assertEquals("שקשוקה", result.value());
        assertFalse(result.isMachineTranslated());
        verifyNoInteractions(translationRepository);
    }

    private static void verifyNoInteractions(Object... mocks) {
        org.mockito.Mockito.verifyNoInteractions(mocks);
    }
}
