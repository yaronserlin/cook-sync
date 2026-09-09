package com.cooksync_server.repositories;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.cooksync_server.entities.RecipeImportJob;

/**
 * Spring Data JPA Repository interface for RecipeImportJob entity management.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
@Repository
public interface RecipeImportJobRepository extends JpaRepository<RecipeImportJob, String> {

    /**
     * Counts how many import jobs a user has started since {@code since}, for per-user rate
     * limiting on this LLM/OCR-cost-bearing operation.
     *
     * @param userId the user to count jobs for
     * @param since the window's start; jobs created before this are not counted
     * @return the number of jobs that user started within the window
     */
    long countByUserIdAndCreatedAtAfter(String userId, LocalDateTime since);

    /**
     * Looks up a job with its owning {@code user} and ordered {@code sourceImages} eagerly
     * fetched, so {@code RecipeImportServiceImp#processImport}'s background pipeline (which runs
     * on its own thread with no surrounding Hibernate session) can read both without triggering a
     * lazy-initialization failure once this call's own short-lived session closes.
     *
     * @param id the job to look up
     * @return the job with its user and source images pre-fetched, if found
     */
    @EntityGraph(attributePaths = {"user", "sourceImages"})
    Optional<RecipeImportJob> findWithUserById(String id);
}
