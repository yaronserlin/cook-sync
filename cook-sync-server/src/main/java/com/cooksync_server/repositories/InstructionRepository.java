package com.cooksync_server.repositories;

import com.cooksync_server.entities.Instruction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Spring Data JPA Repository interface for Instruction entity operations.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 02/08/2026
 */
@Repository
public interface InstructionRepository extends JpaRepository<Instruction, String> {

    /**
     * Retrieves all cooking instruction steps for a recipe sorted sequentially by step number.
     *
     * @param recipeId target recipe unique identifier
     * @return list of instruction entities sorted by step number ascending
     */
    List<Instruction> findByRecipeIdOrderByStepNumberAsc(String recipeId);

    /**
     * Checks that an instruction step exists and belongs to the given recipe.
     *
     * @param id target instruction step ID
     * @param recipeId recipe the instruction must belong to
     * @return true if the instruction exists and is part of that recipe
     */
    boolean existsByIdAndRecipeId(String id, String recipeId);
}