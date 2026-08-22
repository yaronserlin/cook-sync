package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cooksync_server.entities.FavoriteRecipe;
import com.cooksync_server.entities.PersonalInstructionNote;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.UnauthorizedActionException;
import com.cooksync_server.repositories.FavoriteRecipeRepository;
import com.cooksync_server.repositories.PersonalInstructionNoteRepository;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.UserRepository;
import com.dtos.request.note.NoteRequestDTO;
import com.dtos.response.note.NoteResponse;

/**
 * Unit test suite verifying personal note creation, retrieval, and deletion authorization in PersonalNoteService.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 12/08/2026
 */
@ExtendWith(MockitoExtension.class)
class PersonalNoteServiceTest {

    @Mock
    private PersonalInstructionNoteRepository noteRepository;
    @Mock
    private RecipeRepository recipeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FavoriteRecipeRepository favoriteRepository;

    @InjectMocks
    private PersonalNoteService personalNoteService;

    private User sampleUser;
    private Recipe sampleRecipe;
    private UUID recipeUuid;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder().id("user-1").email("gordon@cooksync.com").build();
        recipeUuid = UUID.randomUUID();
        sampleRecipe = Recipe.builder().id(recipeUuid.toString()).title("Beef Wellington").createdBy(sampleUser).build();
    }

    @Test
    void saveNote_ShouldCreateNoteAndAutoFavorite_WhenNoteNonBlankAndNotAlreadyFavorited() {
        NoteRequestDTO request = new NoteRequestDTO(recipeUuid, null, "Reduce salt next time");
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(sampleUser));
        when(recipeRepository.findById(recipeUuid.toString())).thenReturn(Optional.of(sampleRecipe));
        when(noteRepository.findByUserIdAndRecipeIdAndInstructionIdIsNull("user-1", recipeUuid.toString()))
                .thenReturn(Optional.empty());
        when(favoriteRepository.existsByUserIdAndRecipeId("user-1", recipeUuid.toString())).thenReturn(false);

        personalNoteService.saveNote(request, "gordon@cooksync.com");

        verify(noteRepository).save(org.mockito.ArgumentMatchers.any(PersonalInstructionNote.class));
        verify(favoriteRepository).save(org.mockito.ArgumentMatchers.any(FavoriteRecipe.class));
    }

    @Test
    void saveNote_ShouldNotAutoFavorite_WhenAlreadyFavorited() {
        NoteRequestDTO request = new NoteRequestDTO(recipeUuid, null, "Reduce salt next time");
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(sampleUser));
        when(recipeRepository.findById(recipeUuid.toString())).thenReturn(Optional.of(sampleRecipe));
        when(noteRepository.findByUserIdAndRecipeIdAndInstructionIdIsNull("user-1", recipeUuid.toString()))
                .thenReturn(Optional.empty());
        when(favoriteRepository.existsByUserIdAndRecipeId("user-1", recipeUuid.toString())).thenReturn(true);

        personalNoteService.saveNote(request, "gordon@cooksync.com");

        verify(favoriteRepository, never()).save(org.mockito.ArgumentMatchers.any(FavoriteRecipe.class));
    }

    @Test
    void getNote_ShouldReturnNull_WhenNoNoteExists() {
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(sampleUser));
        when(noteRepository.findByUserIdAndRecipeIdAndInstructionIdIsNull("user-1", "recipe-1"))
                .thenReturn(Optional.empty());

        NoteResponse response = personalNoteService.getNote("recipe-1", "gordon@cooksync.com");

        assertNull(response);
    }

    @Test
    void getNote_ShouldReturnNoteResponse_WhenNoteExists() {
        PersonalInstructionNote note = PersonalInstructionNote.builder()
                .id("note-1").user(sampleUser).recipe(sampleRecipe).note("Great crust").build();
        when(userRepository.findByEmail("gordon@cooksync.com")).thenReturn(Optional.of(sampleUser));
        when(noteRepository.findByUserIdAndRecipeIdAndInstructionIdIsNull("user-1", recipeUuid.toString()))
                .thenReturn(Optional.of(note));

        NoteResponse response = personalNoteService.getNote(recipeUuid.toString(), "gordon@cooksync.com");

        assertEquals("note-1", response.id());
        assertEquals("Great crust", response.note());
    }

    @Test
    void deleteNote_ShouldThrowResourceNotFoundException_WhenNoteMissing() {
        when(noteRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> personalNoteService.deleteNote("missing", "gordon@cooksync.com"));
    }

    @Test
    void deleteNote_ShouldThrowUnauthorizedActionException_WhenUserIsNotAuthor() {
        PersonalInstructionNote note = PersonalInstructionNote.builder()
                .id("note-1").user(sampleUser).recipe(sampleRecipe).note("Great crust").build();
        when(noteRepository.findById("note-1")).thenReturn(Optional.of(note));

        assertThrows(UnauthorizedActionException.class,
                () -> personalNoteService.deleteNote("note-1", "someone-else@cooksync.com"));
    }

    @Test
    void deleteNote_ShouldDelete_WhenUserIsAuthor() {
        PersonalInstructionNote note = PersonalInstructionNote.builder()
                .id("note-1").user(sampleUser).recipe(sampleRecipe).note("Great crust").build();
        when(noteRepository.findById("note-1")).thenReturn(Optional.of(note));

        personalNoteService.deleteNote("note-1", "gordon@cooksync.com");

        verify(noteRepository).delete(note);
    }
}
