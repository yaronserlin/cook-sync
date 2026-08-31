package com.cooksync_server.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cooksync_server.constants.PaginationDefaults;
import com.dtos.request.unit.UnitRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.PagedResponse;
import com.dtos.response.unit.UnitResponse;
import com.cooksync_server.services.UnitService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * REST Controller managing measurement unit definitions.
 * Any authenticated user may list units (needed for the recipe wizard's unit picker);
 * creation and deletion require 'ADMIN' authority.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 02/08/2026
 */
@Slf4j
@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    /**
     * Retrieves all measurement units configured in the system.
     *
     * @param page page number index
     * @param size page size limit
     * @return response entity containing list of UnitResponse DTOs
     */
    @GetMapping("")
    public ResponseEntity<ApiResponse<PagedResponse<UnitResponse>>> getAllUnits(
            @RequestParam(defaultValue = PaginationDefaults.DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = PaginationDefaults.DEFAULT_PAGE_SIZE) int size) {
        log.debug("Fetching all units from the system");
        PagedResponse<UnitResponse> units = unitService.getAllUnits(page, size);
        return ResponseEntity.ok(ApiResponse.success(units, "All units retrieved successfully"));
    }

    /**
     * Creates a new measurement unit definition.
     *
     * @param request unit creation request DTO
     * @return response entity containing created UnitResponse DTO
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("")
    public ResponseEntity<ApiResponse<UnitResponse>> createUnit(@Valid @RequestBody UnitRequestDTO request) {
        log.info("Creating new unit: {}", request);
        UnitResponse createdUnit = unitService.createUnit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(createdUnit, "Unit created successfully"));
    }

    /**
     * Deletes a measurement unit by ID.
     *
     * @param id target unit unique identifier
     * @return response entity acknowledging unit deletion
     */
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUnit(@PathVariable String id) {
        log.info("Deleting unit with ID: {}", id);
        unitService.deleteUnit(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Unit deleted successfully"));
    }
}
