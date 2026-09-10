package com.cooksync_server.services;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cooksync_server.entities.ContentTranslation;
import com.cooksync_server.repositories.ContentTranslationRepository;

/**
 * Unit test suite for {@link TranslationCacheInvalidator}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/09/2026
 */
@ExtendWith(MockitoExtension.class)
class TranslationCacheInvalidatorTest {

    @Mock
    private ContentTranslationRepository translationRepository;

    @InjectMocks
    private TranslationCacheInvalidator invalidator;

    @Test
    void invalidate_deletesByEntityTypeAndEntityId() {
        invalidator.invalidate(ContentTranslation.EntityType.RECIPE_TITLE, "recipe-1");

        verify(translationRepository).deleteByEntityTypeAndEntityId(
                ContentTranslation.EntityType.RECIPE_TITLE, "recipe-1");
    }

    @Test
    void invalidate_doesNothing_whenEntityIdIsNull() {
        invalidator.invalidate(ContentTranslation.EntityType.RECIPE_TITLE, null);

        verify(translationRepository, never()).deleteByEntityTypeAndEntityId(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void invalidate_doesNothing_whenEntityIdIsBlank() {
        invalidator.invalidate(ContentTranslation.EntityType.RECIPE_TITLE, "  ");

        verify(translationRepository, never()).deleteByEntityTypeAndEntityId(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void invalidateAll_deletesByEntityTypeAndEntityIdIn() {
        invalidator.invalidateAll(ContentTranslation.EntityType.INGREDIENT_NAME, List.of("ing-1", "ing-2"));

        verify(translationRepository).deleteByEntityTypeAndEntityIdIn(
                ContentTranslation.EntityType.INGREDIENT_NAME, List.of("ing-1", "ing-2"));
    }

    @Test
    void invalidateAll_doesNothing_whenEntityIdsIsNull() {
        invalidator.invalidateAll(ContentTranslation.EntityType.INGREDIENT_NAME, null);

        verify(translationRepository, never()).deleteByEntityTypeAndEntityIdIn(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void invalidateAll_doesNothing_whenEntityIdsIsEmpty() {
        invalidator.invalidateAll(ContentTranslation.EntityType.INGREDIENT_NAME, List.of());

        verify(translationRepository, never()).deleteByEntityTypeAndEntityIdIn(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyList());
    }
}
