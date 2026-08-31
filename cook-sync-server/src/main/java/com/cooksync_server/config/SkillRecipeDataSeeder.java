package com.cooksync_server.config;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cooksync_server.entities.DescriptionBlock;
import com.cooksync_server.entities.Ingredient;
import com.cooksync_server.entities.Instruction;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.RecipeImage;
import com.cooksync_server.entities.Tag;
import com.cooksync_server.entities.Unit;
import com.cooksync_server.entities.User;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.TagRepository;
import com.cooksync_server.repositories.UnitRepository;
import com.cooksync_server.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Minimal, self-contained seeder for local development: seeds only the
 * measurement units and tags actually used below, a single creator account, and
 * the recipes produced via the recipe-to-json skill - not the full
 * 30-recipe/15-user demo dataset in {@link DataSeeder}.
 * <p>
 * Activate with the "seed-skill" Spring profile instead of "seed":
 * {@code mvn spring-boot:run -Dspring-boot.run.profiles=seed-skill}. The two
 * profiles are mutually exclusive (only one seeder should be active at a time)
 * since both wipe the database on startup.
 * <p>
 * Unlike {@link DataSeeder}, this seeder does not re-upload images to
 * Cloudinary - the recipes below already carry real, permanent Cloudinary
 * secure_urls produced by the recipe-to-json skill's own upload flow, so
 * they're used as-is (no {@code Cloudinary} bean dependency needed here).
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 11/08/2026
 */
@Slf4j
@Component
@Profile("seed-skill")
@RequiredArgsConstructor
public class SkillRecipeDataSeeder implements CommandLineRunner {

    private final TagRepository tagRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Entry point invoked by Spring Boot on startup under the "seed-skill" profile. Wipes and
     * repopulates the schema with the minimal dataset: only the units/tags actually referenced
     * below, a single creator account, and the recipe-to-json-skill-generated recipes.
     *
     * @param args command-line arguments, unused
     */
    @Override
    @Transactional
    public void run(String... args) {
        log.info(">>> Seeding minimal dataset: units, tags, one creator user, and skill-generated recipes...");

        clearDatabase();
        List<Unit> units = seedUnits();
        List<Tag> tags = seedTags();
        User creator = seedCreator();

        Map<String, Tag> tagMap = getTagMap(tags);
        Map<String, Unit> unitMap = getUnitMap(units);

        List<Recipe> recipes = new ArrayList<>();
        recipes.add(seedItziksChraimeh(creator, tagMap, unitMap));
        recipes.add(seedShabbatChallah(creator, tagMap, unitMap));
        recipes.add(seedPickledEggplantSalad(creator, tagMap, unitMap));
        recipes.add(seedGlutenFreeOatAndSeedCrackers(creator, tagMap, unitMap));
        recipes.add(seedNoKneadVeganKranzChocolatePeanutButterJamTwistCake(creator, tagMap, unitMap));
        recipes.add(seedNotPitaFermentedRiceQuinoaLentilMungBeanFlatbread(creator, tagMap, unitMap));
        recipeRepository.saveAll(recipes);

        log.info(">>> Seeding completed successfully. Total recipes seeded: {}", recipes.size());
    }

    /**
     * Wipes every table this seeder repopulates, via {@link SeedDatabaseReset}, so each seeding
     * run starts from a clean, deterministic schema state.
     */
    private void clearDatabase() {
        log.info(">>> Clearing existing database tables...");
        SeedDatabaseReset.truncateAllTables(jdbcTemplate);
    }

    /**
     * Seeds only the measurement units actually referenced by the skill-generated recipes below.
     *
     * @return the persisted unit entities
     */
    private List<Unit> seedUnits() {
        log.info(">>> Seeding measurement units used by the skill-generated recipes...");
        return unitRepository.saveAll(List.of(
                Unit.builder().name("Piece").code("piece").build(),
                Unit.builder().name("Tablespoon").code("tbsp").build(),
                Unit.builder().name("Teaspoon").code("tsp").build(),
                Unit.builder().name("Bundle").code("bundle").build(),
                Unit.builder().name("Milliliter").code("ml").build(),
                Unit.builder().name("Gram").code("g").build(),
                Unit.builder().name("Cup").code("cup").build(),
                Unit.builder().name("Clove").code("clove").build()
        ));
    }

    /**
     * Seeds only the tags actually referenced by the skill-generated recipes below.
     *
     * @return the persisted tag entities
     */
    private List<Tag> seedTags() {
        log.info(">>> Seeding tags used by the skill-generated recipes...");
        return tagRepository.saveAll(List.of(
                Tag.builder().name("middle-eastern").build(),
                Tag.builder().name("seafood").build(),
                Tag.builder().name("spicy").build(),
                Tag.builder().name("dinner").build(),
                Tag.builder().name("comfort-food").build(),
                Tag.builder().name("baking").build(),
                Tag.builder().name("vegetarian").build(),
                Tag.builder().name("vegan").build(),
                Tag.builder().name("salad").build(),
                Tag.builder().name("gluten-free").build(),
                Tag.builder().name("healthy").build(),
                Tag.builder().name("dessert").build(),
                Tag.builder().name("high-protein").build()
        ));
    }

    /**
     * Seeds the single non-admin user account that authors every skill-generated recipe.
     *
     * @return the persisted creator user entity
     */
    private User seedCreator() {
        log.info(">>> Seeding creator account...");
        User creator = User.builder()
                .firstName("Noa").lastName("Peretz").email("noa.peretz@cooksync.com")
                .passwordHash(passwordEncoder.encode("Password123!"))
                .isAdmin(false)
                .city("Galilee")
                .bio("\"Vegan through the stomach\" - My mission is to make delicious vegan pastry and give it to everyone to see that vegan can be GOOD!")
                .build();
        return userRepository.save(creator);
    }

    /**
     * Indexes seeded tags by name, so recipe-seeding code can look one up by its literal name
     * instead of tracking each {@link Tag} entity's generated ID by hand.
     *
     * @param tags the seeded tag entities to index
     * @return the tags keyed by {@link Tag#getName()}
     */
    private Map<String, Tag> getTagMap(List<Tag> tags) {
        return tags.stream().collect(Collectors.toMap(Tag::getName, t -> t, (a, b) -> a));
    }

    /**
     * Indexes seeded units by their lowercased code, so recipe-seeding code can look one up by
     * its literal code instead of tracking each {@link Unit} entity's generated ID by hand.
     *
     * @param units the seeded unit entities to index
     * @return the units keyed by their lowercased {@link Unit#getCode()}
     */
    private Map<String, Unit> getUnitMap(List<Unit> units) {
        return units.stream().collect(Collectors.toMap(u -> u.getCode().toLowerCase(), u -> u, (a, b) -> a));
    }

    /**
     * Looks up a seeded unit by its code. Unlike {@link DataSeeder#getUnit}, there is no
     * {@code "piece"} fallback here: a missing code is logged and {@code null} is returned as-is,
     * since every code referenced by the skill-generated recipes below is expected to exist in
     * {@link #seedUnits()}.
     *
     * @param unitMap the seeded units, as built by {@link #getUnitMap(List)}
     * @param code the unit code to look up
     * @return the matching unit, or {@code null} if no unit has that code
     */
    private Unit getUnit(Map<String, Unit> unitMap, String code) {
        Unit unit = unitMap.get(code.toLowerCase());
        if (unit == null) {
            log.warn("Unit code '{}' not found in unitMap.", code);
        }
        return unit;
    }

    /**
     * Builds an (unsaved) ingredient entity for a seeded recipe; persisted later via
     * {@code Recipe}'s cascading save in {@link #run(String...)}.
     *
     * @param name the ingredient display name
     * @param qtyStr the ingredient quantity, as a decimal string parsed into a {@link BigDecimal}
     * @param unit the resolved measurement unit, typically via {@link #getUnit(Map, String)}
     * @return the built (not yet persisted) ingredient entity
     */
    private Ingredient createIng(String name, String qtyStr, Unit unit) {
        return Ingredient.builder()
                .name(name)
                .quantity(new BigDecimal(qtyStr))
                .unit(unit)
                .build();
    }

    /**
     * Builds an in-memory instruction-step descriptor for a seeded recipe, later converted into a
     * persisted {@code Instruction} entity by {@link #createRecipe} once the parent recipe and
     * its ingredient list both exist.
     *
     * @param stepNum the 1-based sequential position of the step
     * @param desc the step's instruction text
     * @param hasTimer whether the step requires a countdown timer
     * @param timeSec the timer duration in seconds, or {@code null} if {@code hasTimer} is {@code false}
     * @param imgUrl an illustrative image URL for this step, used as-is, or {@code null} for none
     * @param linkedIngIndices 0-based indices into the recipe's ingredient list identifying which
     *                         ingredients this step uses, or none if the step uses no ingredients
     * @return the built step descriptor
     */
    private InstructionStepData createStep(int stepNum, String desc, boolean hasTimer, Integer timeSec, String imgUrl, Integer... linkedIngIndices) {
        return new InstructionStepData(stepNum, desc, hasTimer, timeSec, imgUrl, linkedIngIndices == null ? List.of() : Arrays.asList(linkedIngIndices));
    }

    /**
     * In-memory descriptor for one seeded recipe's instruction step, built by
     * {@link #createStep} and consumed by {@link #createRecipe} to construct the persisted
     * {@code Instruction} entity once the parent recipe's ingredient indices can be resolved.
     *
     * @param stepNumber the 1-based sequential position of the step
     * @param description the step's instruction text
     * @param hasTimer whether the step requires a countdown timer
     * @param timeSeconds the timer duration in seconds, or {@code null} if {@code hasTimer} is {@code false}
     * @param imageUrl an illustrative image URL for this step, used as-is, or {@code null} for none
     * @param linkedIngredientIndices 0-based indices into the recipe's ingredient list identifying this step's ingredients
     */
    private record InstructionStepData(int stepNumber, String description, boolean hasTimer, Integer timeSeconds, String imageUrl, List<Integer> linkedIngredientIndices) {

    }

    /**
     * In-memory descriptor for one explicit, per-recipe description block (not a fixed 3-block
     * template as in {@link DataSeeder}), consumed by {@link #createRecipe} to build the
     * persisted {@code DescriptionBlock} entities in author-given order.
     *
     * @param type the block's content-type discriminator (TEXT or IMAGE)
     * @param text the block's prose content, populated when {@code type} is TEXT
     * @param imageUrl the block's image resource URL, populated when {@code type} is IMAGE
     * @param caption an optional image caption, only meaningful when {@code type} is IMAGE
     */
    private record DescriptionBlockData(DescriptionBlock.BlockType type, String text, String imageUrl, String caption) {

    }

    /**
     * Builds, wires, and persists a complete Recipe entity from skill-generated data. Unlike
     * {@link DataSeeder#createRecipe}, no Cloudinary upload happens here: {@code primaryImageUrl}
     * and every description/instruction image URL are already permanent Cloudinary
     * {@code secure_url}s produced by the recipe-to-json skill, so they are used as-is. The
     * recipe's own {@code description} field is derived the same way {@code RecipeServiceImp}
     * does for live API submissions: the first TEXT description block's text, or {@code ""}.
     *
     * @param title the recipe's display title
     * @param difficulty the recipe's skill difficulty level
     * @param prepTime preparation duration in minutes
     * @param cookTime active cooking duration in minutes
     * @param servings recommended serving yield count
     * @param creator the seeded user to set as the recipe's author
     * @param tags the seeded tag entities to associate with the recipe
     * @param primaryImageUrl the Cloudinary URL used as-is for the recipe's cover image
     * @param ingredients the recipe's ingredient entities, as built by {@link #createIng}
     * @param stepDataList the recipe's instruction steps, as built by {@link #createStep}
     * @param descriptionBlockData the recipe's description blocks, in author-given order
     * @return the persisted recipe entity
     */
    private Recipe createRecipe(String title, Recipe.Difficulty difficulty, int prepTime, int cookTime,
            int servings, User creator, List<Tag> tags, String primaryImageUrl,
            List<Ingredient> ingredients, List<InstructionStepData> stepDataList,
            List<DescriptionBlockData> descriptionBlockData) {

        String description = descriptionBlockData.stream()
                .filter(b -> b.type() == DescriptionBlock.BlockType.TEXT)
                .map(DescriptionBlockData::text)
                .filter(t -> t != null && !t.isBlank())
                .findFirst()
                .orElse("");

        Recipe recipe = Recipe.builder()
                .title(title)
                .description(description)
                .difficulty(difficulty)
                .prepTimeMinutes(prepTime)
                .cookTimeMinutes(cookTime)
                .servings(servings)
                .createdBy(creator)
                .tags(new LinkedHashSet<>(tags))
                .build();

        // Wire ingredients
        Set<Ingredient> ingredientSet = new LinkedHashSet<>();
        for (Ingredient ing : ingredients) {
            ing.setRecipe(recipe);
            ingredientSet.add(ing);
        }
        recipe.setIngredients(ingredientSet);

        // Wire instructions, linking each step to ingredients by 0-based index into `ingredients` above
        List<Ingredient> ingList = new ArrayList<>(ingredients);
        Set<Instruction> instructionSet = new LinkedHashSet<>();
        for (InstructionStepData step : stepDataList) {
            Set<Ingredient> stepIngs = new LinkedHashSet<>();
            for (Integer idx : step.linkedIngredientIndices()) {
                if (idx != null && idx >= 0 && idx < ingList.size()) {
                    stepIngs.add(ingList.get(idx));
                }
            }
            Instruction instruction = Instruction.builder()
                    .stepNumber(step.stepNumber())
                    .description(step.description())
                    .hasTimer(step.hasTimer())
                    .timeSeconds(step.timeSeconds())
                    .imageUrl(step.imageUrl())
                    .ingredients(stepIngs)
                    .recipe(recipe)
                    .build();
            instructionSet.add(instruction);
        }
        recipe.setInstructions(instructionSet);

        // Build description blocks exactly as given - no fixed template
        List<DescriptionBlock> blocks = new ArrayList<>();
        int sortOrder = 0;
        for (DescriptionBlockData b : descriptionBlockData) {
            blocks.add(DescriptionBlock.builder()
                    .recipe(recipe)
                    .type(b.type())
                    .text(b.text())
                    .imageUrl(b.imageUrl())
                    .caption(b.caption())
                    .sortOrder(sortOrder++)
                    .build());
        }
        recipe.setDescriptionBlocks(blocks);

        // Primary cover image, if any (real Cloudinary URL, used as-is)
        Set<RecipeImage> images = new LinkedHashSet<>();
        if (primaryImageUrl != null && !primaryImageUrl.isBlank()) {
            images.add(RecipeImage.builder()
                    .recipe(recipe)
                    .imageUrl(primaryImageUrl)
                    .isPrimary(true)
                    .build());
        }
        recipe.setImages(images);

        return recipe;
    }

    /**
     * Builds and persists the skill-generated "Itzik's Chraimeh" recipe, a spicy North
     * African-style fish-in-tomato-pepper sauce dish.
     *
     * @param creator the seeded user to set as the recipe's author
     * @param tagMap the seeded tags, indexed via {@link #getTagMap(List)}
     * @param unitMap the seeded units, indexed via {@link #getUnitMap(List)}
     * @return the persisted recipe entity
     */
    private Recipe seedItziksChraimeh(User creator, Map<String, Tag> tagMap, Map<String, Unit> unitMap) {
        List<Ingredient> ingredients = List.of(
                createIng("Bell peppers", "4", getUnit(unitMap, "piece")),
                createIng("Tomatoes", "5", getUnit(unitMap, "piece")),
                createIng("Paprika", "5", getUnit(unitMap, "tsp")),
                createIng("Table salt", "2", getUnit(unitMap, "tsp")),
                createIng("Black pepper", "1", getUnit(unitMap, "tsp")),
                createIng("Olive oil", "2", getUnit(unitMap, "tbsp")),
                createIng("Cilantro", "1", getUnit(unitMap, "bundle")),
                createIng("Hot pepper", "1", getUnit(unitMap, "piece")),
                createIng("Garlic", "6", getUnit(unitMap, "clove"))
        );

        List<InstructionStepData> steps = List.of(
                createStep(1, "Heat the olive oil in a pot over high heat. Add the hot pepper and bell peppers, and cover with a lid.", false, null, null, 0, 7, 5),
                createStep(2, "Once it becomes fragrant, flip the peppers to their other side and lower the heat.", false, null, null, 0, 7),
                createStep(3, "Add the tomatoes, garlic, paprika, and cilantro. Simmer for 30 minutes, then add fish balls or fish pieces.", true, 1800, null, 1, 2, 6, 8)
        );

        List<DescriptionBlockData> blocks = List.of(
                new DescriptionBlockData(DescriptionBlock.BlockType.TEXT,
                        "Itzik's chraimeh — a spicy North African-style tomato sauce built on garlic, cilantro, paprika, and sweet peppers, simmered until rich and finished with fish balls or fish cooked right in the sauce.",
                        null, null)
        );

        return createRecipe("Itzik's Chraimeh", Recipe.Difficulty.EASY, 15, 40, 4, creator,
                List.of(tagMap.get("middle-eastern"), tagMap.get("seafood"), tagMap.get("spicy"), tagMap.get("dinner")),
                null, ingredients, steps, blocks);
    }

    /**
     * Builds and persists the skill-generated "Shabbat Challah" recipe, a braided egg-washed
     * yeasted bread.
     *
     * @param creator the seeded user to set as the recipe's author
     * @param tagMap the seeded tags, indexed via {@link #getTagMap(List)}
     * @param unitMap the seeded units, indexed via {@link #getUnitMap(List)}
     * @return the persisted recipe entity
     */
    private Recipe seedShabbatChallah(User creator, Map<String, Tag> tagMap, Map<String, Unit> unitMap) {
        List<Ingredient> ingredients = List.of(
                createIng("Egg (for egg wash)", "1", getUnit(unitMap, "piece")),
                createIng("Lukewarm water", "200", getUnit(unitMap, "ml")),
                createIng("Sugar", "4", getUnit(unitMap, "tbsp")),
                createIng("Regular flour", "500", getUnit(unitMap, "g")),
                createIng("Salt", "1", getUnit(unitMap, "tsp")),
                createIng("Sesame seeds", "1", getUnit(unitMap, "tbsp")),
                createIng("Dry yeast (or 25g fresh yeast)", "1", getUnit(unitMap, "tbsp")),
                createIng("Oil (for egg wash)", "1", getUnit(unitMap, "tsp")),
                createIng("Egg", "1", getUnit(unitMap, "piece")),
                createIng("Oil", "0.25", getUnit(unitMap, "cup"))
        );

        String coverImg = "https://res.cloudinary.com/dg6fhhm3e/image/upload/v1786443895/CookSyncApp/recipes/k50uzwfjkt9u0kc66kdy.jpg";
        String step1Img = "https://res.cloudinary.com/dg6fhhm3e/image/upload/v1786443899/CookSyncApp/recipes/im49c3uijwqjggqjpfm6.jpg";
        String step3Img = "https://res.cloudinary.com/dg6fhhm3e/image/upload/v1786443901/CookSyncApp/recipes/gb9jdfzfr8pyyev1gzun.jpg";
        String step4Img = "https://res.cloudinary.com/dg6fhhm3e/image/upload/v1786443906/CookSyncApp/recipes/i7mitf5fs1csbpabgj9m.jpg";
        String step6Img = "https://res.cloudinary.com/dg6fhhm3e/image/upload/v1786443947/CookSyncApp/recipes/i2n84y5hegxyfqqkzdx9.gif";
        String step7Img = "https://res.cloudinary.com/dg6fhhm3e/image/upload/v1786443950/CookSyncApp/recipes/g3r3brxateijxc1002vk.jpg";
        String step8Img = "https://res.cloudinary.com/dg6fhhm3e/image/upload/v1786443953/CookSyncApp/recipes/esvo8b4oyogkk0ifelwi.jpg";
        String step9Img = "https://res.cloudinary.com/dg6fhhm3e/image/upload/v1786443956/CookSyncApp/recipes/ufjtxrrz9ooagbb7hj1p.jpg";
        String crumbImg = "https://res.cloudinary.com/dg6fhhm3e/image/upload/v1786443960/CookSyncApp/recipes/bsk6xbs6su74zhqlorbt.jpg";

        List<InstructionStepData> steps = List.of(
                createStep(1, "In a large bowl, mix the flour, sugar, and dry yeast together. Add the egg, oil, and lukewarm water, and stir with a spoon until the mixture starts to come together. (Alternative: instead of dry yeast, crumble 25g fresh yeast into a small bowl with 1 tablespoon of the sugar, add half the water, stir, and let sit 10-15 minutes until foamy, then mix into the flour with the remaining sugar, egg, oil, and water.)", false, null, step1Img, 2, 3, 1, 6, 8, 9),
                createStep(2, "Knead by hand for 3-4 minutes (or use a stand mixer with a dough hook). If the dough feels sticky, add a little more flour; if it's too dry, add a touch more water.", true, 240, null),
                createStep(3, "Sprinkle the salt evenly over the dough and continue kneading for another 4-5 minutes, until the dough is smooth and uniform.", true, 300, step3Img, 4),
                createStep(4, "Cover the bowl with a towel and let the dough rise in a warm place for 1 to 1.5 hours, until doubled in volume.", true, 5400, step4Img),
                createStep(5, "On a lightly floured surface, divide the dough into 4 equal pieces and roll each piece into a long strand about 55-60 cm long.", false, null, null),
                createStep(6, "Twist two strands together in a loose 'screw' pattern, then coil into a spiral (like a snail shell) — keep it loose, not tight, so the challah has room to rise. Repeat with the remaining two strands to shape the second challah.", false, null, step6Img),
                createStep(7, "Line a baking sheet with parchment paper and place the shaped challahs on it. Cover with a towel and let rise again in a warm place for about 1 to 1.5 hours, until doubled in volume.", true, 5400, step7Img),
                createStep(8, "Brush the challahs with the egg wash (1 beaten egg mixed with 1 teaspoon oil) and sprinkle with sesame seeds.", false, null, step8Img, 0, 5, 7),
                createStep(9, "Preheat the oven to 180°C (350°F) for at least 7 minutes. Once hot, bake the challahs for about 30 minutes, until golden brown.", true, 1800, step9Img)
        );

        List<DescriptionBlockData> blocks = List.of(
                new DescriptionBlockData(DescriptionBlock.BlockType.TEXT,
                        "An easy, airy, and delicious Shabbat challah, walked through step by step. It takes a little more effort than a typical quick recipe, but stays simple and manageable — and the fresh-yeast version comes out especially light and flavorful.",
                        null, null),
                new DescriptionBlockData(DescriptionBlock.BlockType.IMAGE, null, crumbImg, "Soft, airy crumb once baked and torn open")
        );

        return createRecipe("Shabbat Challah", Recipe.Difficulty.MEDIUM, 10, 30, 8, creator,
                List.of(tagMap.get("comfort-food"), tagMap.get("baking"), tagMap.get("vegetarian")),
                coverImg, ingredients, steps, blocks);
    }

    /**
     * Builds and persists the skill-generated "Pickled Eggplant Salad" recipe, a
     * vinegar-and-olive-oil dressed eggplant salad.
     *
     * @param creator the seeded user to set as the recipe's author
     * @param tagMap the seeded tags, indexed via {@link #getTagMap(List)}
     * @param unitMap the seeded units, indexed via {@link #getUnitMap(List)}
     * @return the persisted recipe entity
     */
    private Recipe seedPickledEggplantSalad(User creator, Map<String, Tag> tagMap, Map<String, Unit> unitMap) {
        List<Ingredient> ingredients = List.of(
                createIng("Parsley, finely chopped", "0.5", getUnit(unitMap, "cup")),
                createIng("Lemon (juiced)", "0.5", getUnit(unitMap, "piece")),
                createIng("Black pepper", "0.25", getUnit(unitMap, "tsp")),
                createIng("Olive oil", "4", getUnit(unitMap, "tbsp")),
                createIng("Vinegar", "4", getUnit(unitMap, "tbsp")),
                createIng("Paprika", "0.25", getUnit(unitMap, "tsp")),
                createIng("Eggplant", "2", getUnit(unitMap, "piece")),
                createIng("Maple syrup, honey, or silan (date syrup)", "1", getUnit(unitMap, "tsp")),
                createIng("Garlic, minced", "3", getUnit(unitMap, "clove")),
                createIng("Salt", "1", getUnit(unitMap, "tsp")),
                createIng("Cold water", "4", getUnit(unitMap, "tbsp"))
        );

        String coverImg = "https://res.cloudinary.com/dg6fhhm3e/image/upload/v1786443201/CookSyncApp/recipes/w2ytmwph9sp98egnbbeu.jpg";

        List<InstructionStepData> steps = List.of(
                createStep(1, "Slice the eggplants into thin rounds. Salt the slices to draw out their liquid, then dry them thoroughly with paper towels or a clean kitchen towel.", false, null, null, 6),
                createStep(2, "Fry the eggplant slices in regular oil until golden, then transfer to paper towels to drain. Alternative: to bake instead, cut the slices about 1.5 cm thick, brush each one with olive oil, and bake at 200°C (400°F) until golden.", false, null, null, 3, 6),
                createStep(3, "In a bowl, mix together the olive oil, cold water, vinegar, lemon juice, maple syrup (or honey or silan), minced garlic, chopped parsley, salt, pepper, and paprika. Set the pickle mixture aside for 10 minutes.", true, 600, null, 0, 1, 2, 3, 4, 5, 7, 9, 10, 8),
                createStep(4, "Arrange the fried (or baked) eggplant slices neatly and pour the pickle mixture between the layers. It tastes great after resting for 1 hour, and even better after 4 hours — though in our house it's usually gone within 5 minutes!", true, 3600, null, 0, 1, 2, 3, 4, 5, 6, 7, 9, 10, 8)
        );

        List<DescriptionBlockData> blocks = List.of(
                new DescriptionBlockData(DescriptionBlock.BlockType.TEXT,
                        "A vibrant Middle Eastern pickled eggplant salad: thin eggplant slices fried (or baked) until golden, then layered with a garlicky olive oil, vinegar, and lemon pickle brimming with fresh parsley. It's best after resting a few hours, but it rarely lasts that long.",
                        null, null),
                new DescriptionBlockData(DescriptionBlock.BlockType.IMAGE, null, coverImg, "Pickled eggplant slices layered with the tehmitz marinade")
        );

        return createRecipe("Pickled Eggplant Salad (Tehmitz)", Recipe.Difficulty.EASY, 20, 15, 4, creator,
                List.of(tagMap.get("vegan"), tagMap.get("salad"), tagMap.get("gluten-free"), tagMap.get("vegetarian"), tagMap.get("healthy"), tagMap.get("middle-eastern")),
                coverImg, ingredients, steps, blocks);
    }

    /**
     * Builds and persists the skill-generated "Gluten-Free Oat and Seed Crackers" recipe, a
     * baked mixed-seed cracker made with lentil/chickpea flour and gluten-free oats.
     *
     * @param creator the seeded user to set as the recipe's author
     * @param tagMap the seeded tags, indexed via {@link #getTagMap(List)}
     * @param unitMap the seeded units, indexed via {@link #getUnitMap(List)}
     * @return the persisted recipe entity
     */
    private Recipe seedGlutenFreeOatAndSeedCrackers(User creator, Map<String, Tag> tagMap, Map<String, Unit> unitMap) {
        List<Ingredient> ingredients = List.of(
                createIng("Red lentil flour (or chickpea flour)", "1", getUnit(unitMap, "cup")),
                createIng("Quick-cooking oats (gluten-free variety available)", "1", getUnit(unitMap, "cup")),
                createIng("Sunflower seeds", "0.5", getUnit(unitMap, "cup")),
                createIng("Sesame seeds (regular or unhulled)", "0.5", getUnit(unitMap, "cup")),
                createIng("Black sesame seeds or nigella seeds", "0.25", getUnit(unitMap, "cup")),
                createIng("Sliced almonds", "0.33", getUnit(unitMap, "cup")),
                createIng("Pumpkin seeds", "0.33", getUnit(unitMap, "cup")),
                createIng("Whole poppy seeds (not ground)", "3", getUnit(unitMap, "tbsp")),
                createIng("Garlic powder", "1", getUnit(unitMap, "tbsp")),
                createIng("Salt", "1", getUnit(unitMap, "tbsp")),
                createIng("Olive oil", "3", getUnit(unitMap, "tbsp")),
                createIng("Water", "500", getUnit(unitMap, "ml"))
        );

        List<InstructionStepData> steps = List.of(
                createStep(1, "Preheat the oven to 160°C (320°F).", false, null, null),
                createStep(2, "Combine all the ingredients in a bowl, in the order listed, and mix well with a spoon until all the flour is fully absorbed into the batter. The batter should come out quite watery — that's normal, it's meant to be that way.", false, null, null, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11),
                createStep(3, "Pour half the batter onto a baking tray lined with parchment paper and spread it out evenly until the paper is fully covered. This amount is enough for exactly 2 standard oven trays, so if you run short before covering the tray, spread it thinner.", false, null, null),
                createStep(4, "Bake for 30 minutes, then flip the tray over.", true, 1800, null),
                createStep(5, "Continue baking for another 30 minutes, or until deep golden and completely crisp. If the layer wasn't spread to an even thickness, some parts will finish sooner than others — check regularly and make sure to bake until it is fully crisp.", true, 1800, null),
                createStep(6, "Remove from the oven, let cool slightly, then break freely into crackers. If it doesn't snap cleanly with a crisp crunch, return it to the oven for a few more minutes.", false, null, "https://res.cloudinary.com/dg6fhhm3e/image/upload/v1786453353/cooksync/991bc485-a94d-4848-9c15-78c71ef7e641/Gluten_Free_Oat_and_Seed_Crackers/instruction_6_1786453334985.jpg")
        );

        List<DescriptionBlockData> blocks = List.of(
                new DescriptionBlockData(DescriptionBlock.BlockType.TEXT, "Gluten-free crackers made with oats and seeds — simple to prepare, wonderfully crispy, and they keep beautifully in a sealed container for up to two weeks.", null, null),
                new DescriptionBlockData(DescriptionBlock.BlockType.IMAGE, null, "https://res.cloudinary.com/dg6fhhm3e/image/upload/v1786453334/cooksync/991bc485-a94d-4848-9c15-78c71ef7e641/Gluten_Free_Oat_and_Seed_Crackers/description_991bc485-a94d-4848-9c15-78c71ef7e641_1786453316226.jpg", null)
        );

        return createRecipe("Gluten-Free Oat and Seed Crackers", Recipe.Difficulty.EASY, 20, 60, 8, creator,
                List.of(tagMap.get("gluten-free"), tagMap.get("vegan"), tagMap.get("healthy"), tagMap.get("baking")),
                "https://res.cloudinary.com/dg6fhhm3e/image/upload/v1786453315/cooksync/991bc485-a94d-4848-9c15-78c71ef7e641/Gluten_Free_Oat_and_Seed_Crackers/main_991bc485-a94d-4848-9c15-78c71ef7e641_1786453289042.jpg", ingredients, steps, blocks);
    }

    /**
     * Builds and persists the skill-generated "No-Knead Vegan Kranz" recipe, a braided
     * chocolate-peanut-butter-and-jam twist cake made with a no-knead vegan dough.
     *
     * @param creator the seeded user to set as the recipe's author
     * @param tagMap the seeded tags, indexed via {@link #getTagMap(List)}
     * @param unitMap the seeded units, indexed via {@link #getUnitMap(List)}
     * @return the persisted recipe entity
     */
    private Recipe seedNoKneadVeganKranzChocolatePeanutButterJamTwistCake(User creator, Map<String, Tag> tagMap, Map<String, Unit> unitMap) {
        List<Ingredient> ingredients = List.of(
                createIng("White flour", "500", getUnit(unitMap, "g")),
                createIng("Instant dry yeast", "6", getUnit(unitMap, "g")),
                createIng("White sugar (for the dough)", "80", getUnit(unitMap, "g")),
                createIng("Vegetable oil (for the dough)", "80", getUnit(unitMap, "g")),
                createIng("Water (for the dough)", "160", getUnit(unitMap, "g")),
                createIng("Plant-based milk (for the dough)", "160", getUnit(unitMap, "g")),
                createIng("Salt", "0.5", getUnit(unitMap, "tsp")),
                createIng("Dark chocolate (for the chocolate filling)", "150", getUnit(unitMap, "g")),
                createIng("Vegetable oil (for the chocolate filling)", "50", getUnit(unitMap, "g")),
                createIng("Cocoa powder (for the chocolate filling)", "50", getUnit(unitMap, "g")),
                createIng("Sugar (for the chocolate filling)", "100", getUnit(unitMap, "g")),
                createIng("Chopped chocolate (for sprinkling in the filling)", "50", getUnit(unitMap, "g")),
                createIng("Peanut butter (for the peanut butter & jam filling)", "250", getUnit(unitMap, "g")),
                createIng("Jam (for the peanut butter & jam filling)", "250", getUnit(unitMap, "g")),
                createIng("Sugar (for the sugar syrup)", "100", getUnit(unitMap, "g")),
                createIng("Water (for the sugar syrup)", "120", getUnit(unitMap, "g"))
        );

        List<InstructionStepData> steps = List.of(
                createStep(1, "Make the dough: Pour the flour, sugar, and yeast into a mixing bowl and stir together. Make a well in the center and pour in the rest of the dough ingredients (oil, water, plant milk, salt). Mix everything with a spoon or spatula until fairly uniform — it won't be as smooth as a kneaded dough, but everything should come together; that's fine.", false, null, null, 0, 1, 2, 3, 4, 5, 6),
                createStep(2, "First rise: Cover the bowl and let it rise at room temperature for 2–4 hours. (Alternatively: let it sit out for 15 minutes, then refrigerate for 8–24 hours — it may not fully double, but will be easier to work with. For a faster rise, place it in an oven that's off or on its lowest setting with a bowl of boiling water at the bottom, which rises it in about 45 minutes — but it will be too soft and sticky to roll, so chill it in the freezer for 15 minutes afterward before shaping.)", true, 10800, null),
                createStep(3, "Make the chocolate filling: Weigh the chocolate filling ingredients — except the chopped chocolate — into a bowl and microwave for about 30 seconds to a minute, stirring until fully melted. Refrigerate for about 20 minutes until spreadable (the outer edges may firm up faster than the center — stir well and chill a little longer if needed).", true, 1200, null, 7, 8, 9, 10),
                createStep(4, "Make the syrup: Melt the water and sugar together in a small pot; turn off the heat as soon as all the sugar has dissolved (stir a little once it's partly dissolved).", false, null, null, 14, 15),
                createStep(5, "Work the dough: After the first rise, flour your work surface, the dough, and your hands. Turn the dough out onto the floured surface. Pull the dough from the sides and bottom up toward the top center, repeatedly, reflouring the surface as it gets exposed, until the dough is a little less sticky and a bit smoother. Divide it in half; return one half to the bowl and cover it.", false, null, null),
                createStep(6, "Roll and fill the first half: Work the first half of the dough a little more. Flour the work surface again, place the dough in the center, and flour the top. Roll it into a rectangle about 0.5cm thick (keep flouring top and bottom so it doesn't stick — no-knead dough is stickier than classic yeast dough, and this is what keeps it workable). Spread about half the chocolate filling over the whole rectangle. Fold it envelope-style: bring the top edge down toward the bottom, but not all the way, then lift the bottom edge up over the top. Roll the folded dough out to about half its thickness (it's fine if some filling squeezes out or the top tears a little). Spread the remaining chocolate filling over the dough, leaving one edge clean, then scatter the chopped chocolate on top. Roll the dough up toward the clean edge (dampen the edge slightly with a finger to help it seal). Cut the rolled log in half lengthwise, turn the two halves so the cut side faces up, then twist one over the other to form a braided twist. Carefully transfer to the loaf pan.", false, null, null, 11),
                createStep(7, "Second rise: Cover and let rise at room temperature for 30–40 minutes, until doubled in size.", true, 2100, null),
                createStep(8, "Repeat with the second half: Repeat steps 6 and 7 with the second half of the dough. This time, spread peanut butter over the first rolled rectangle, and after folding and re-rolling, spread the jam on top before rolling up, cutting, and twisting as before.", true, 2100, null, 12, 13),
                createStep(9, "Preheat the oven to 180°C (355°F) fan/turbo. Brush the loaves with a little plant-based milk. Bake for about 35 minutes, until nicely browned.", true, 2100, null),
                createStep(10, "As soon as the loaves come out of the oven, generously brush them with the sugar syrup — don't wait.", false, null, null)
        );

        List<DescriptionBlockData> blocks = List.of(
                new DescriptionBlockData(DescriptionBlock.BlockType.TEXT, "A rolled, twisted yeasted kranz cake — like a babka — made from a fuss-free no-knead dough. Instead of true laminated dough, the dough is rolled and filled twice to build thin layers with plenty of filling, then twisted into a braid and baked. This recipe makes two loaves: one with a rich chocolate filling, the other with peanut butter and jam, both brushed with sugar syrup straight out of the oven.", null, null),
                new DescriptionBlockData(DescriptionBlock.BlockType.IMAGE, null, "https://res.cloudinary.com/dg6fhhm3e/image/upload/v1786454529/cooksync/991bc485-a94d-4848-9c15-78c71ef7e641/No_Knead_Vegan_Kranz___Chocolate___Peanut_Butter_Jam_Twist_Cake/description_991bc485-a94d-4848-9c15-78c71ef7e641_1786454527405.jpg", "The peanut butter & jam variant, syrup-glazed straight out of the oven.")
        );

        return createRecipe("No-Knead Vegan Kranz — Chocolate & Peanut Butter-Jam Twist Cake", Recipe.Difficulty.HARD, 45, 35, 16, creator,
                List.of(tagMap.get("vegan"), tagMap.get("baking"), tagMap.get("dessert"), tagMap.get("comfort-food")),
                "https://res.cloudinary.com/dg6fhhm3e/image/upload/v1786454526/cooksync/991bc485-a94d-4848-9c15-78c71ef7e641/No_Knead_Vegan_Kranz___Chocolate___Peanut_Butter_Jam_Twist_Cake/main_991bc485-a94d-4848-9c15-78c71ef7e641_1786454524808.jpg", ingredients, steps, blocks);
    }

    /**
     * Builds and persists the skill-generated "Not-Pita" recipe, a fermented flatbread batter
     * made from blended rice, quinoa, lentils, and mung beans.
     *
     * @param creator the seeded user to set as the recipe's author
     * @param tagMap the seeded tags, indexed via {@link #getTagMap(List)}
     * @param unitMap the seeded units, indexed via {@link #getUnitMap(List)}
     * @return the persisted recipe entity
     */
    private Recipe seedNotPitaFermentedRiceQuinoaLentilMungBeanFlatbread(User creator, Map<String, Tag> tagMap, Map<String, Unit> unitMap) {
        List<Ingredient> ingredients = List.of(
                createIng("Brown rice", "1", getUnit(unitMap, "cup")),
                createIng("Quinoa", "1", getUnit(unitMap, "cup")),
                createIng("Red lentils (or lentils of choice)", "0.5", getUnit(unitMap, "cup")),
                createIng("Mung beans", "0.5", getUnit(unitMap, "cup")),
                createIng("Water (for blending)", "7", getUnit(unitMap, "cup")),
                createIng("Olive oil (for the batter)", "1", getUnit(unitMap, "tbsp")),
                createIng("Salt", "1", getUnit(unitMap, "tsp")),
                createIng("Olive oil (for greasing the tray)", "1", getUnit(unitMap, "tbsp")),
                createIng("Za'atar (optional topping)", "1", getUnit(unitMap, "tsp")),
                createIng("Nigella seeds (optional topping)", "1", getUnit(unitMap, "tsp")),
                createIng("Sesame seeds (optional topping)", "1", getUnit(unitMap, "tsp")),
                createIng("Cumin seeds (optional topping)", "1", getUnit(unitMap, "tsp"))
        );

        List<InstructionStepData> steps = List.of(
                createStep(1, "Combine the brown rice, quinoa, red lentils, and mung beans in a large bowl, cover generously with water, and let them soak together overnight.", true, 28800, null, 0, 1, 2, 3),
                createStep(2, "In the morning, drain and blend everything together with about 7 cups of fresh water into a smooth batter.", false, null, null, 4),
                createStep(3, "Transfer the batter to a bowl or container and let it ferment at room temperature for a few hours.", true, 14400, null),
                createStep(4, "Mix in a little olive oil and salt to taste.", false, null, null, 5, 6),
                createStep(5, "Line a baking tray with parchment paper, brush lightly with olive oil, and pour the batter in a thin, even layer.", false, null, null, 7),
                createStep(6, "Sprinkle the top with za'atar, nigella seeds, sesame seeds, or cumin seeds — whichever combination you like.", false, null, null, 8, 9, 10, 11),
                createStep(7, "Bake for about 20 minutes at 200–220°C (390–430°F), until set and lightly golden.", true, 1200, null)
        );

        List<DescriptionBlockData> blocks = List.of(
                new DescriptionBlockData(DescriptionBlock.BlockType.TEXT, "A gluten-free flatbread made from soaked, fermented rice, quinoa, lentils, and mung beans — blended into a batter, baked thin, and topped with za'atar, nigella, sesame, or cumin seeds. Not actually pita, but a nourishing, protein-rich stand-in.", null, null)
        );

        return createRecipe("\"Not Pita\" Fermented Rice, Quinoa, Lentil & Mung Bean Flatbread", Recipe.Difficulty.EASY, 15, 20, 6, creator,
                List.of(tagMap.get("vegan"), tagMap.get("gluten-free"), tagMap.get("healthy"), tagMap.get("high-protein")),
                null, ingredients, steps, blocks);
    }
}
