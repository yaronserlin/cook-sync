package com.cooksync_server.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.cooksync_server.entities.Unit;
import com.cooksync_server.exceptions.ResourceAlreadyExistsException;
import com.cooksync_server.exceptions.ResourceInUseException;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.repositories.IngredientRepository;
import com.cooksync_server.repositories.UnitRepository;
import com.dtos.request.unit.UnitRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.unit.UnitResponse;

/**
 * Unit test suite verifying measurement unit retrieval, creation uniqueness, and deletion in UnitServiceImp.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 12/08/2026
 */
@ExtendWith(MockitoExtension.class)
class UnitServiceTest {

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private IngredientRepository ingredientRepository;

    @InjectMocks
    private UnitServiceImp unitService;

    private Unit sampleUnit;

    @BeforeEach
    void setUp() {
        sampleUnit = Unit.builder().id("unit-1").code("g").name("Gram").namePlural("Grams").build();
    }

    @Test
    void getAllUnits_ShouldReturnPagedResponse() {
        Page<Unit> page = new PageImpl<>(java.util.List.of(sampleUnit), PageRequest.of(0, 10), 1);
        when(unitRepository.findAll(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(page);

        PagedResponse<UnitResponse> response = unitService.getAllUnits(0, 10);

        assertEquals(1, response.content().size());
        assertEquals("Gram", response.content().get(0).name());
    }

    @Test
    void createUnit_ShouldThrowResourceAlreadyExistsException_WhenCodeTaken() {
        UnitRequestDTO request = new UnitRequestDTO("Gram", "Grams", "g");
        when(unitRepository.existsByCodeIgnoreCase("g")).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> unitService.createUnit(request));
    }

    @Test
    void createUnit_ShouldThrowResourceAlreadyExistsException_WhenNameTaken() {
        UnitRequestDTO request = new UnitRequestDTO("Gram", "Grams", "g");
        when(unitRepository.existsByCodeIgnoreCase("g")).thenReturn(false);
        when(unitRepository.existsByNameIgnoreCase("Gram")).thenReturn(true);

        assertThrows(ResourceAlreadyExistsException.class, () -> unitService.createUnit(request));
    }

    @Test
    void createUnit_ShouldSaveUnit_WhenCodeAndNameAvailable() {
        UnitRequestDTO request = new UnitRequestDTO("Gram", "Grams", "g");
        when(unitRepository.existsByCodeIgnoreCase("g")).thenReturn(false);
        when(unitRepository.existsByNameIgnoreCase("Gram")).thenReturn(false);
        when(unitRepository.save(org.mockito.ArgumentMatchers.any(Unit.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UnitResponse response = unitService.createUnit(request);

        assertEquals("g", response.code());
        assertEquals("Gram", response.name());
        assertEquals("Grams", response.namePlural());
    }

    @Test
    void deleteUnit_ShouldThrowResourceNotFoundException_WhenMissing() {
        when(unitRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> unitService.deleteUnit("missing"));
    }

    @Test
    void deleteUnit_ShouldDelete_WhenFound() {
        when(unitRepository.findById("unit-1")).thenReturn(Optional.of(sampleUnit));
        when(ingredientRepository.countByUnitId("unit-1")).thenReturn(0L);

        unitService.deleteUnit("unit-1");

        verify(unitRepository).delete(sampleUnit);
    }

    @Test
    void deleteUnit_ShouldThrowResourceInUseException_WhenIngredientsReferenceIt() {
        when(unitRepository.findById("unit-1")).thenReturn(Optional.of(sampleUnit));
        when(ingredientRepository.countByUnitId("unit-1")).thenReturn(3L);

        assertThrows(ResourceInUseException.class, () -> unitService.deleteUnit("unit-1"));

        verify(unitRepository, org.mockito.Mockito.never()).delete(sampleUnit);
    }
}
