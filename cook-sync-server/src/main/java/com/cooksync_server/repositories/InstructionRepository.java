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
}