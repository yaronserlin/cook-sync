package com.cooksync_server.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cooksync_server.services.RecipeImportService;
import com.dtos.request.recipeimport.RecipeImportStartRequestDTO;
import com.dtos.response.ApiResponse;
import com.dtos.response.recipeimport.RecipeImportJobResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST Controller for starting and polling smart recipe-import jobs (web page or photo).
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
@RestController
@RequestMapping("/api/recipe-imports")
@RequiredArgsConstructor
public class RecipeImportController {

    private final RecipeImportService recipeImportService;

    /**
     * Starts a new import job and returns immediately with its (initially {@code PENDING})
     * status — extraction runs in the background.
     *
     * @param request the import request
     * @param authentication active user authentication token
     * @return response entity containing the newly created job's status
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RecipeImportJobResponse>> startImport(
            @Valid @RequestBody RecipeImportStartRequestDTO request,
            Authentication authentication) {
        RecipeImportJobResponse job = recipeImportService.startImport(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(job, "Import started"));
    }

    /**
     * Retrieves a job's current status, for the client to poll.
     *
     * @param jobId target job unique identifier
     * @param authentication active user authentication token
     * @return response entity containing the job's current status
     */
    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<RecipeImportJobResponse>> getJobStatus(
            @PathVariable String jobId,
            Authentication authentication) {
        RecipeImportJobResponse job = recipeImportService.getJobStatus(jobId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(job, "Import job status retrieved"));
    }
}
