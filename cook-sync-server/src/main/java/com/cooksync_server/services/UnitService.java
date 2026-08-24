package com.cooksync_server.services;

import com.dtos.request.unit.UnitRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.unit.UnitResponse;

/**
 * Service interface for managing measurement unit definitions.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
public interface UnitService {

    /**
     * Retrieves a paginated list of all measurement units configured in the system.
     *
     * @param page page number index
     * @param size page size limit
     * @return PagedResponse of UnitResponse DTOs
     */
    PagedResponse<UnitResponse> getAllUnits(int page, int size);

    /**
     * Creates a new measurement unit definition, ensuring code and name uniqueness.
     *
     * @param request unit creation request DTO
     * @return UnitResponse DTO of the created unit
     * @throws com.cooksync_server.exceptions.ResourceAlreadyExistsException if a unit with the same code or name already exists
     */
    UnitResponse createUnit(UnitRequestDTO request);

    /**
     * Deletes a measurement unit by ID.
     *
     * @param id target unit ID
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if no unit with the given ID exists
     */
    void deleteUnit(String id);
}
