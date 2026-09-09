package com.cooksync_server.services;

import com.dtos.request.recipeimport.RecipeImportStartRequestDTO;
import com.dtos.response.recipeimport.RecipeImportJobResponse;

/**
 * Service interface for starting and tracking smart recipe-import jobs (web page or photo).
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public interface RecipeImportService {

    /**
     * Starts a new import job: validates the request, applies per-user rate limiting, persists
     * the job as {@code PENDING}, and dispatches background processing — returning immediately
     * rather than waiting for extraction to finish.
     *
     * @param request the import request (source type + URL or already-uploaded photo URL)
     * @param userEmail the requesting user's email
     * @return the newly created job, still {@code PENDING}
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if the user cannot be found
     * @throws com.cooksync_server.exceptions.RateLimitExceededException if the user has started
     *         too many import jobs recently
     * @throws IllegalArgumentException if the request is missing the URL/photo its source type requires
     */
    RecipeImportJobResponse startImport(RecipeImportStartRequestDTO request, String userEmail);

    /**
     * Retrieves a job's current status.
     *
     * @param jobId the job to look up
     * @param userEmail the requesting user's email — must be the job's owner
     * @return the job's current status
     * @throws com.cooksync_server.exceptions.ResourceNotFoundException if the job or user cannot be found
     * @throws com.cooksync_server.exceptions.auth.UnauthorizedActionException if the requester doesn't own the job
     */
    RecipeImportJobResponse getJobStatus(String jobId, String userEmail);
}
