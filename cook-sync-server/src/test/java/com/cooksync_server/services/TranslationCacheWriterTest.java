package com.cooksync_server.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cooksync_server.entities.ContentTranslation;
import com.cooksync_server.repositories.ContentTranslationRepository;
import com.cooksync_server.repositories.TranslationMemoryRepository;

/**
 * Unit test suite for {@link TranslationCacheWriter}. Verifies the right repository methods are
 * invoked with the right arguments — the {@code REQUIRES_NEW} transaction propagation and the
 * native {@code insertIfAbsent} SQL's idempotency itself are real transaction/database semantics
 * that a Mockito mock cannot exercise, consistent with this project's existing all-Mockito test
 * convention (confirmed: no {@code @DataJpaTest}/{@code @SpringBootTest} precedent exists).
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
@ExtendWith(MockitoExtension.class)
class TranslationCacheWriterTest {

    @Mock
    private ContentTranslationRepository translationRepository;
    @Mock
    private TranslationMemoryRepository translationMemoryRepository;

    @InjectMocks
    private TranslationCacheWriter cacheWriter;

    @Test
    void save_insertsIntoBothTheEntityCacheAndTheSharedMemory() {
        ContentTranslation translation = ContentTranslation.builder()
                .entityType(ContentTranslation.EntityType.RECIPE_TITLE)
                .entityId("recipe-1")
                .locale("he")
                .value("שקשוקה")
                .source(ContentTranslation.Source.MACHINE)
                .build();

        cacheWriter.save(translation, "hash-abc");

        verify(translationRepository).insertIfAbsent(
                anyString(), eq("RECIPE_TITLE"), eq("recipe-1"), eq("he"), eq("שקשוקה"), eq("MACHINE"), any(LocalDateTime.class));
        verify(translationMemoryRepository).insertIfAbsent(
                anyString(), eq("hash-abc"), eq("he"), eq("שקשוקה"), eq("MACHINE"), any(LocalDateTime.class));
    }

    @Test
    void copyToEntityCache_insertsOnlyIntoTheEntityCache_notSharedMemory() {
        cacheWriter.copyToEntityCache(ContentTranslation.EntityType.INGREDIENT_NAME, "ing-1", "he", "קמח",
                ContentTranslation.Source.MACHINE);

        verify(translationRepository).insertIfAbsent(
                anyString(), eq("INGREDIENT_NAME"), eq("ing-1"), eq("he"), eq("קמח"), eq("MACHINE"), any(LocalDateTime.class));
        verify(translationMemoryRepository, org.mockito.Mockito.never()).insertIfAbsent(
                anyString(), anyString(), anyString(), anyString(), anyString(), any(LocalDateTime.class));
    }
}
