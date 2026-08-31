package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.exceptions.auth.UnauthorizedActionException;
import com.cooksync_server.repositories.UserRepository;

/**
 * Unit test suite verifying the owner-or-administrator authorization checks in
 * {@link OwnershipValidator}, shared by the recipe, ingredient, instruction, and review services.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 31/08/2026
 */
@ExtendWith(MockitoExtension.class)
class OwnershipValidatorTest {

    @Mock
    private UserRepository userRepository;

    private User owner;
    private User nonOwnerAdmin;
    private User nonOwnerNonAdmin;

    @BeforeEach
    void setUp() {
        owner = User.builder().id("user-1").email("owner@cooksync.com").isAdmin(false).build();
        nonOwnerAdmin = User.builder().id("user-2").email("admin@cooksync.com").isAdmin(true).build();
        nonOwnerNonAdmin = User.builder().id("user-3").email("other@cooksync.com").isAdmin(false).build();
    }

    @Test
    void requireOwnerOrAdmin_ShouldNotThrow_WhenCurrentUserIsOwner() {
        assertDoesNotThrow(() -> OwnershipValidator.requireOwnerOrAdmin("user-1", owner, "Not authorized"));
    }

    @Test
    void requireOwnerOrAdmin_ShouldThrowUnauthorizedActionException_WhenCurrentUserIsNeitherOwnerNorAdmin() {
        assertThrows(UnauthorizedActionException.class,
                () -> OwnershipValidator.requireOwnerOrAdmin("user-1", nonOwnerNonAdmin, "Not authorized"));
    }

    @Test
    void requireOwnerOrAdmin_ShouldNotThrow_WhenCurrentUserIsNotOwnerButIsAdmin() {
        assertDoesNotThrow(() -> OwnershipValidator.requireOwnerOrAdmin("user-1", nonOwnerAdmin, "Not authorized"));
    }

    @Test
    void requireOwnedResource_ShouldReturnResource_WhenCurrentUserIsOwner() {
        String resource = "the-resource";
        when(userRepository.findByEmail("owner@cooksync.com")).thenReturn(Optional.of(owner));

        String result = OwnershipValidator.requireOwnedResource(
                () -> Optional.of(resource), "Recipe", "recipe-1",
                r -> "user-1", userRepository, "owner@cooksync.com", "Not authorized");

        assertEquals(resource, result);
    }

    @Test
    void requireOwnedResource_ShouldThrowUnauthorizedActionException_WhenCurrentUserIsNeitherOwnerNorAdmin() {
        String resource = "the-resource";
        when(userRepository.findByEmail("other@cooksync.com")).thenReturn(Optional.of(nonOwnerNonAdmin));

        assertThrows(UnauthorizedActionException.class, () -> OwnershipValidator.requireOwnedResource(
                () -> Optional.of(resource), "Recipe", "recipe-1",
                r -> "user-1", userRepository, "other@cooksync.com", "Not authorized"));
    }

    @Test
    void requireOwnedResource_ShouldReturnResource_WhenCurrentUserIsNotOwnerButIsAdmin() {
        String resource = "the-resource";
        when(userRepository.findByEmail("admin@cooksync.com")).thenReturn(Optional.of(nonOwnerAdmin));

        String result = OwnershipValidator.requireOwnedResource(
                () -> Optional.of(resource), "Recipe", "recipe-1",
                r -> "user-1", userRepository, "admin@cooksync.com", "Not authorized");

        assertEquals(resource, result);
    }

    @Test
    void requireOwnedResource_ShouldThrowResourceNotFoundException_WhenResourceMissing() {
        assertThrows(ResourceNotFoundException.class, () -> OwnershipValidator.requireOwnedResource(
                Optional::empty, "Recipe", "recipe-1",
                r -> "user-1", userRepository, "owner@cooksync.com", "Not authorized"));
    }

    @Test
    void requireOwnedResource_ShouldThrowResourceNotFoundException_WhenCurrentUserMissing() {
        String resource = "the-resource";
        when(userRepository.findByEmail("missing@cooksync.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> OwnershipValidator.requireOwnedResource(
                () -> Optional.of(resource), "Recipe", "recipe-1",
                r -> "user-1", userRepository, "missing@cooksync.com", "Not authorized"));
    }
}
