package com.cooksync_server.services;

import com.dtos.request.unit.UnitRequestDTO;
import com.dtos.response.PagedResponse;
import com.dtos.response.unit.UnitResponse;

/**
 * Service interface for managing measurement unit definitions.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
public interface IUnitService {

    /**
     * Retrieves a paginated list of all measurement units configured in the system.
     *
     * @param page page number index
     * @param size page size limit
     * @return PagedResponse of UnitResponse DTOs
     */
    PagedResponse<UnitResponse> getAllUnits(int page, int size);

    /**
     * Retrieves a measurement unit by ID.
     *
     * @param id target unit ID
     * @return UnitResponse DTO
     */
    UnitResponse getUnitById(String id);

    /**
     * Creates a new measurement unit definition, ensuring code and name uniqueness.
     *
     * @param request unit creation request DTO
     * @return UnitResponse DTO of the created unit
     */
    UnitResponse createUnit(UnitRequestDTO request);

    /**
     * Deletes a measurement unit by ID.
     *
     * @param id target unit ID
     */
    void deleteUnit(String id);
}
