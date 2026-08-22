package com.cooksync_server.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.dtos.request.unit.UnitRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.unit.UnitResponse;
import com.cooksync_server.entities.Unit;
import com.cooksync_server.exceptions.ResourceAllReadyExistsException;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.mappers.UnitMapper;
import com.cooksync_server.repositories.UnitRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service class managing measurement unit creation, retrieval, and deletion.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Service
@RequiredArgsConstructor
public class UnitServiceImp implements UnitService{

    private final UnitRepository unitRepository;

    /**
     * Retrieves all measurement units configured in the system.
     *
     * Complexity:
     * Time: O(U) where U is total unit count
     * Space: O(U)
     *
     * @return list of UnitResponse DTOs
     */
    @Transactional(readOnly = true)
    public PagedResponse<UnitResponse> getAllUnits(int page, int size) {
        return PagedResponseMapper.findAllPaged(unitRepository, page, size, UnitMapper::toResponse);
    }

    /**
     * Retrieves a measurement unit by ID.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param id target unit ID
     * @return UnitResponse DTO
     */
    @Transactional(readOnly = true)
    public UnitResponse getUnitById(String id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", id));
        return UnitMapper.toResponse(unit);
    }

    /**
     * Creates a new measurement unit definition ensuring code uniqueness.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param request unit creation request DTO
     * @return UnitResponse DTO of created unit
     */
    @Transactional
    public UnitResponse createUnit(UnitRequestDTO request) {
        String formattedCode = request.code().toLowerCase().trim();
        String formattedName = StringUtils.capitalize(request.name().toLowerCase().trim());

        if (unitRepository.existsByCodeIgnoreCase(formattedCode)) {
            throw new ResourceAllReadyExistsException("Unit code '" + formattedCode + "'", formattedCode);
        }
        if (unitRepository.existsByNameIgnoreCase(formattedName)) {
            throw new ResourceAllReadyExistsException("Unit name '" + formattedName + "'", formattedName);
        }

        Unit newUnit = Unit.builder()
                .name(formattedName)
                .code(formattedCode)
                .build();

        return UnitMapper.toResponse(unitRepository.save(newUnit));
    }

    /**
     * Deletes a measurement unit by ID.
     *
     * Complexity:
     * Time: O(1)
     * Space: O(1)
     *
     * @param id target unit ID
     */
    @Transactional
    public void deleteUnit(String id) {
        Unit unit = unitRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unit", id));
        unitRepository.delete(unit);
    }
}
