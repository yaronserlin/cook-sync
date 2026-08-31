package com.cooksync_server.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.cloudinary.Cloudinary;
import com.cooksync_server.entities.DescriptionBlock;
import com.cooksync_server.entities.FavoriteRecipe;
import com.cooksync_server.entities.Ingredient;
import com.cooksync_server.entities.Instruction;
import com.cooksync_server.entities.PersonalInstructionNote;
import com.cooksync_server.entities.Recipe;
import com.cooksync_server.entities.RecipeImage;
import com.cooksync_server.entities.Review;
import com.cooksync_server.entities.Tag;
import com.cooksync_server.entities.Unit;
import com.cooksync_server.entities.User;
import com.cooksync_server.repositories.FavoriteRecipeRepository;
import com.cooksync_server.repositories.PersonalInstructionNoteRepository;
import com.cooksync_server.repositories.RecipeImageRepository;
import com.cooksync_server.repositories.RecipeRepository;
import com.cooksync_server.repositories.ReviewRepository;
import com.cooksync_server.repositories.TagRepository;
import com.cooksync_server.repositories.UnitRepository;
import com.cooksync_server.repositories.UserRepository;
import com.cooksync_server.services.CloudinaryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Data Seeder component for initializing the database under the 'seed' active profile.
 * Populates 30 authentic culinary recipes, realistic user profiles, detailed step instructions with
 * timers, reviews, favorites, and personal notes. When Cloudinary credentials
 * ({@code CLOUDINARY_CLOUD_NAME}/{@code CLOUDINARY_API_KEY}/{@code CLOUDINARY_API_SECRET}) are
 * configured, every referenced media asset (avatars, recipe covers, step photos) is uploaded to
 * Cloudinary; otherwise the seeder detects the missing credentials up front, skips upload
 * attempts entirely, and seeds using the original stock image URLs directly, so the app runs
 * without a Cloudinary account.
 *
 * @author Yaron Serlin
 * @version 2.0
 * @since 10/08/2026
 */
@Slf4j
@Component
@Profile("seed")
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TagRepository tagRepository;
    private final UnitRepository unitRepository;
    private final UserRepository userRepository;
    private final RecipeRepository recipeRepository;
    private final ReviewRepository reviewRepository;
    private final FavoriteRecipeRepository favoriteRecipeRepository;
    private final PersonalInstructionNoteRepository personalInstructionNoteRepository;
    private final RecipeImageRepository recipeImageRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;
    private final CloudinaryService cloudinaryService;
    private final Cloudinary cloudinary;

    /** In-memory cache of remote image URL -> Cloudinary secure URL to prevent redundant uploads. */
    private final Map<String, String> cloudinaryCache = new ConcurrentHashMap<>();

    /**
     * Entry point invoked by Spring Boot on startup under the "seed" profile. Wipes and
     * repopulates the entire schema with the full demo dataset (units, tags, users, recipes,
     * reviews, favorites, personal notes), uploading every referenced image to Cloudinary along
     * the way.
     *
     * @param args command-line arguments, unused
     */
    @Override
    @Transactional
    public void run(String... args) {
        if (isCloudinaryConfigured()) {
            log.info(">>> Starting database reset and realistic seeding with Cloudinary media upload...");
        } else {
            log.warn(">>> Cloudinary credentials are not configured (CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET). "
                    + "Seeding will use the original stock image URLs directly, skipping upload.");
        }

        clearDatabase();
        List<Unit> units = seedUnits();
        List<Tag> tags = seedTags();
        List<User> users = seedUsers();
        List<Recipe> recipes = seedRecipes(users, units, tags);
        seedReviews(recipes, users);
        seedFavorites(recipes, users);
        seedPersonalNotes(recipes, users);

        log.info(">>> Database reset and seeding completed successfully. Total recipes seeded: {}", recipes.size());
    }

    /**
     * Reports whether real Cloudinary credentials are present. When they are not (e.g. local
     * development or CI without a Cloudinary account), the seeder skips upload attempts entirely
     * rather than making network calls that are guaranteed to fail.
     *
     * @return {@code true} if cloud name, API key, and API secret are all non-blank
     */
    private boolean isCloudinaryConfigured() {
        return cloudinary != null
                && cloudinary.config.cloudName != null && !cloudinary.config.cloudName.isBlank()
                && cloudinary.config.apiKey != null && !cloudinary.config.apiKey.isBlank()
                && cloudinary.config.apiSecret != null && !cloudinary.config.apiSecret.isBlank();
    }

    /**
     * Uploads a remote image URL to Cloudinary and returns the generated secure Cloudinary URL.
     * If Cloudinary is not configured or the upload fails, gracefully falls back to the original URL.
     *
     * @param imageUrl original image HTTP URL
     * @param folder Cloudinary target folder (e.g., "[baseFolder]/[userEmail]/avatar", "[baseFolder]/[userEmail]/[recipeTitle]")
     * @param publicId exact Cloudinary public ID name
     * @return Cloudinary secure URL or fallback original URL
     */
    private String uploadToCloudinary(String imageUrl, String folder, String publicId) {
        if (imageUrl == null || imageUrl.isBlank() || !isCloudinaryConfigured()) {
            return imageUrl;
        }
        String cacheKey = folder + "/" + publicId + ":" + imageUrl;
        if (cloudinaryCache.containsKey(cacheKey)) {
            return cloudinaryCache.get(cacheKey);
        }

        try {
            Map<String, Object> options = new HashMap<>();
            options.put("folder", folder);
            options.put("public_id", publicId);
            options.put("overwrite", true);
            options.put("resource_type", "auto");

            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(imageUrl, options);
            String secureUrl = (String) uploadResult.get("secure_url");
            if (secureUrl != null && !secureUrl.isBlank()) {
                log.info("Uploaded to Cloudinary: [{}] -> [{}] (Folder: {}, PublicID: {})", imageUrl, secureUrl, folder, publicId);
                cloudinaryCache.put(cacheKey, secureUrl);
                return secureUrl;
            }
        } catch (Exception e) {
            log.warn("Cloudinary upload failed for [{}] (Folder: {}, PublicID: {}): {}. Using original URL fallback.",
                    imageUrl, folder, publicId, e.getMessage());
        }

        cloudinaryCache.put(cacheKey, imageUrl);
        return imageUrl;
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
     * Seeds the fixed catalog of measurement units used by the sample recipes below.
     *
     * @return the persisted unit entities
     */
    private List<Unit> seedUnits() {
        log.info(">>> Seeding measurement units...");
        return unitRepository.saveAll(List.of(
                Unit.builder().name("Cup").code("cup").build(),
                Unit.builder().name("Tablespoon").code("tbsp").build(),
                Unit.builder().name("Teaspoon").code("tsp").build(),
                Unit.builder().name("Gram").code("g").build(),
                Unit.builder().name("Kilogram").code("kg").build(),
                Unit.builder().name("Milliliter").code("ml").build(),
                Unit.builder().name("Liter").code("l").build(),
                Unit.builder().name("Pinch").code("pinch").build(),
                Unit.builder().name("Clove").code("clove").build(),
                Unit.builder().name("Piece").code("piece").build(),
                Unit.builder().name("Slice").code("slice").build(),
                Unit.builder().name("Can").code("can").build(),
                Unit.builder().name("Package").code("pkg").build(),
                Unit.builder().name("Handful").code("handful").build(),
                Unit.builder().name("Sprig").code("sprig").build(),
                Unit.builder().name("Bundle").code("bundle").build()
        ));
    }

    /**
     * Seeds the fixed catalog of recipe tags, including a handful of deliberate near-duplicate
     * (space- vs. hyphen-separated) variants so the admin duplicate-tag detection and merge
     * tools have real groups to exercise.
     *
     * @return the persisted tag entities
     */
    private List<Tag> seedTags() {
        log.info(">>> Seeding recipe tags...");
        return tagRepository.saveAll(List.of(
                Tag.builder().name("vegan").build(),
                Tag.builder().name("quick").build(),
                Tag.builder().name("healthy").build(),
                Tag.builder().name("breakfast").build(),
                Tag.builder().name("dinner").build(),
                Tag.builder().name("dessert").build(),
                Tag.builder().name("gluten-free").build(),
                Tag.builder().name("high-protein").build(),
                Tag.builder().name("comfort-food").build(),
                Tag.builder().name("spicy").build(),
                Tag.builder().name("vegetarian").build(),
                Tag.builder().name("italian").build(),
                Tag.builder().name("asian").build(),
                Tag.builder().name("mexican").build(),
                Tag.builder().name("middle-eastern").build(),
                Tag.builder().name("french").build(),
                Tag.builder().name("soup").build(),
                Tag.builder().name("salad").build(),
                Tag.builder().name("baking").build(),
                Tag.builder().name("seafood").build(),
                // Space-separated variants of existing hyphenated tags, so the admin
                // duplicate-tag tools (AdminServiceImp#getDuplicateTagGroups / mergeTags) have
                // real groups to detect and merge. They must differ by separator character,
                // not just casing: tags.name has a case-insensitive unique constraint
                // (utf8mb4_unicode_ci), so a same-case-insensitive-string variant like
                // "Gluten Free" vs "gluten-free" would fail to insert.
                Tag.builder().name("gluten free").build(),
                Tag.builder().name("high protein").build(),
                Tag.builder().name("comfort food").build()
        ));
    }

    /**
     * Seeds the fixed catalog of sample user accounts (including one admin), uploading a stock
     * avatar image to Cloudinary for each one via {@link #uploadToCloudinary(String, String, String)}.
     *
     * @return the persisted user entities
     */
    private List<User> seedUsers() {
        log.info(">>> Seeding users and uploading avatar media to Cloudinary...");

        String[] rawAvatars = {
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1504593811423-6dd665756598?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1488426862026-3ee34a7d66df?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1544723795-3fb6469f5b39?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=400&q=80",
                "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?auto=format&fit=crop&w=400&q=80"
        };

        List<User> initialUsers = List.of(
                User.builder().firstName("Yaron").lastName("Serlin").email("yaron@cooksync.com")
                        .passwordHash(passwordEncoder.encode("123456aA!")).isAdmin(true)
                        .city("Tel Aviv").bio("Executive chef & food scientist passionate about Mediterranean and Italian cuisine.").build(),
                User.builder().firstName("Admin").lastName("User").email("admin@cooksync.com")
                        .passwordHash(passwordEncoder.encode("Password123!")).isAdmin(true)
                        .city("Jerusalem").bio("CookSync Platform Administrator & recipe curator.").build(),
                User.builder().firstName("Chef").lastName("John").email("chef@cooksync.com")
                        .passwordHash(passwordEncoder.encode("Password123!")).isAdmin(false)
                        .city("Haifa").bio("Professional pastry chef and French culinary enthusiast.").build(),
                User.builder().firstName("Maya").lastName("Levi").email("maya@cooksync.com")
                        .passwordHash(passwordEncoder.encode("Password123!")).isAdmin(false)
                        .city("Tel Aviv").bio("Plant-based recipe creator and wellness blog.").build(),
                User.builder().firstName("Noam").lastName("Cohen").email("noam@cooksync.com")
                        .passwordHash(passwordEncoder.encode("Password123!")).isAdmin(false)
                        .city("Ramat Gan").bio("Asian street food addict and home fermenter.").build(),
                User.builder().firstName("Sara").lastName("Green").email("sara@cooksync.com")
                        .passwordHash(passwordEncoder.encode("Password123!")).isAdmin(false)
                        .city("Herzliya").bio("Weekend baker, sourdough nerd, and dessert lover.").build(),
                User.builder().firstName("Eli").lastName("Sharon").email("eli@cooksync.com")
                        .passwordHash(passwordEncoder.encode("Password123!")).isAdmin(false)
                        .city("Beer Sheva").bio("BBQ pitmaster and slow-cooked meat connoisseur.").build(),
                User.builder().firstName("Ari").lastName("Levy").email("ari@cooksync.com")
                        .passwordHash(passwordEncoder.encode("Password123!")).isAdmin(false)
                        .city("Netanya").bio("Seafood addict obsessed with authentic Spanish paella.").build(),
                User.builder().firstName("Noa").lastName("Aviv").email("noa@cooksync.com")
                        .passwordHash(passwordEncoder.encode("Password123!")).isAdmin(false)
                        .city("Ra'anana").bio("Quick 15-minute weeknight dinner innovator for busy parents.").build(),
                User.builder().firstName("Lior").lastName("Ben").email("lior@cooksync.com")
                        .passwordHash(passwordEncoder.encode("Password123!")).isAdmin(false)
                        .city("Rishon LeZion").bio("Craft burger enthusiast and homemade sauce collector.").build(),
                User.builder().firstName("Dana").lastName("Mor").email("dana@cooksync.com")
                        .passwordHash(passwordEncoder.encode("Password123!")).isAdmin(false)
                        .city("Givatayim").bio("Nutritionist crafting high-protein meal prep recipes.").build(),
                User.builder().firstName("Guy").lastName("Eldar").email("guy@cooksync.com")
                        .passwordHash(passwordEncoder.encode("Password123!")).isAdmin(false)
                        .city("Eilat").bio("Mexican taco fanatic and spice explorer.").build(),
                User.builder().firstName("Eden").lastName("Nir").email("eden@cooksync.com")
                        .passwordHash(passwordEncoder.encode("Password123!")).isAdmin(false)
                        .city("Hod HaSharon").bio("Gluten-free baker sharing cozy comfort food recipes.").build(),
                User.builder().firstName("Yossi").lastName("Amit").email("yossi@cooksync.com")
                        .passwordHash(passwordEncoder.encode("Password123!")).isAdmin(false)
                        .city("Ashdod").bio("Traditional Middle Eastern home cook preserving family recipes.").build(),
                User.builder().firstName("Neta").lastName("Carmi").email("neta@cooksync.com")
                        .passwordHash(passwordEncoder.encode("Password123!")).isAdmin(false)
                        .city("Rehovot").bio("Italian pasta purist making fresh tagliatelle from scratch.").build()
        );

        List<User> savedUsers = userRepository.saveAll(initialUsers);

        for (int i = 0; i < savedUsers.size(); i++) {
            User user = savedUsers.get(i);
            long currentTime = System.currentTimeMillis();
            String folder = cloudinaryService.buildUserFolder(user.getEmail(), "avatar");
            String publicId = String.format("%s_%s_%s_%d",
                    user.getFirstName(), user.getLastName(), user.getId(), currentTime);
            String uploadedAvatar = uploadToCloudinary(rawAvatars[i % rawAvatars.length], folder, publicId);
            user.setAvatarUrl(uploadedAvatar);
            user.setShowFavoritesPublicly(i % 2 == 0);
        }

        return userRepository.saveAll(savedUsers);
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
     * Looks up a seeded unit by its code, falling back to {@code "piece"} (and, if that is also
     * unavailable, to an arbitrary seeded unit) so a typo or omission in the hardcoded sample
     * recipe data below never crashes the seeding run.
     *
     * @param unitMap the seeded units, as built by {@link #getUnitMap(List)}
     * @param code the unit code to look up, or {@code null} to use the {@code "piece"} fallback directly
     * @return the matching unit, the {@code "piece"} fallback, or an arbitrary seeded unit if neither is found
     */
    private Unit getUnit(Map<String, Unit> unitMap, String code) {
        if (code == null) {
            return unitMap.get("piece");
        }
        Unit unit = unitMap.get(code.toLowerCase());
        if (unit == null) {
            log.warn("Unit code '{}' not found in unitMap, using fallback 'piece'", code);
            unit = unitMap.get("piece");
        }
        if (unit == null && !unitMap.isEmpty()) {
            unit = unitMap.values().iterator().next();
        }
        return unit;
    }

    /**
     * Seeds a fixed catalog of 30 fully-populated sample recipes — each with ingredients,
     * instruction steps (some with timers), tags, and Cloudinary-hosted cover/gallery images —
     * spread across the seeded sample users as authors.
     *
     * @param users the seeded user entities to assign as recipe authors
     * @param units the seeded unit entities, indexed via {@link #getUnitMap(List)} for ingredient lookups
     * @param tags the seeded tag entities, indexed via {@link #getTagMap(List)} for recipe tagging
     * @return the persisted recipe entities
     */
    private List<Recipe> seedRecipes(List<User> users, List<Unit> units, List<Tag> tags) {
        log.info(">>> Seeding 30 authentic recipes with ingredients, timers, and Cloudinary media...");

        Map<String, Tag> tagMap = getTagMap(tags);
        Map<String, Unit> unitMap = getUnitMap(units);

        List<Recipe> recipeList = new ArrayList<>();

        // 1. Classic Spaghetti Carbonara
        recipeList.add(createRecipe(
                "Classic Spaghetti Carbonara",
                "Authentic Roman pasta Carbonara made with crispy guanciale, egg yolks, freshly grated Pecorino Romano cheese, and cracked black pepper.",
                Recipe.Difficulty.MEDIUM, 10, 15, 2, users.get(0),
                List.of(tagMap.get("italian"), tagMap.get("dinner"), tagMap.get("comfort-food"), tagMap.get("high-protein")),
                "https://images.unsplash.com/photo-1612874742237-6526221588e3?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1551183053-bf91a1d81141?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Spaghetti", "200", getUnit(unitMap, "g")),
                        createIng("Guanciale", "100", getUnit(unitMap, "g")),
                        createIng("Egg Yolks", "4", getUnit(unitMap, "piece")),
                        createIng("Pecorino Romano", "50", getUnit(unitMap, "g")),
                        createIng("Black Pepper", "1", getUnit(unitMap, "tsp"))
                ),
                List.of(
                        createStep(1, "Bring a large pot of salted water to a boil and cook spaghetti until al dente.", true, 540, "https://images.unsplash.com/photo-1551183053-bf91a1d81141?auto=format&fit=crop&w=1200&q=80", 0),
                        createStep(2, "Crisp sliced guanciale in a skillet over medium heat until golden and fat renders.", true, 360, null, 1),
                        createStep(3, "Whisk egg yolks with grated Pecorino Romano and freshly cracked black pepper until creamy.", false, null, null, 2, 3, 4),
                        createStep(4, "Toss hot drained pasta into the skillet, remove from heat, and quickly stir in the egg mixture to create a glossy emulsion.", false, null, null)
                )
        ));

        // 2. Authentic Middle Eastern Shakshuka
        recipeList.add(createRecipe(
                "Authentic Middle Eastern Shakshuka",
                "Poached eggs in a rich simmered tomato, bell pepper, and garlic sauce spiced with cumin and smoked paprika, topped with crumbled feta.",
                Recipe.Difficulty.EASY, 10, 20, 3, users.get(13),
                List.of(tagMap.get("middle-eastern"), tagMap.get("breakfast"), tagMap.get("vegetarian"), tagMap.get("spicy")),
                "https://images.unsplash.com/photo-1590412200988-a436970781fa?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1590412200988-a436970781fa?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Eggs", "5", getUnit(unitMap, "piece")),
                        createIng("Tomatoes", "4", getUnit(unitMap, "piece")),
                        createIng("Red Bell Pepper", "1", getUnit(unitMap, "piece")),
                        createIng("Garlic Cloves", "3", getUnit(unitMap, "clove")),
                        createIng("Cumin", "1", getUnit(unitMap, "tsp")),
                        createIng("Smoked Paprika", "1", getUnit(unitMap, "tsp")),
                        createIng("Crumbled Feta Cheese", "80", getUnit(unitMap, "g"))
                ),
                List.of(
                        createStep(1, "Sauté chopped red bell pepper, onion, and minced garlic cloves in olive oil until tender.", true, 300, null, 2, 3),
                        createStep(2, "Add diced tomatoes, cumin, and smoked paprika. Simmer until sauce thickens.", true, 600, "https://images.unsplash.com/photo-1590412200988-a436970781fa?auto=format&fit=crop&w=1200&q=80", 1, 4, 5),
                        createStep(3, "Make small wells in the sauce, crack in the eggs, cover, and gently poach until the whites are set. Finish with a scatter of crumbled feta cheese.", true, 420, null, 0, 6)
                )
        ));

        // 3. Japanese Chicken Teriyaki Bowl
        recipeList.add(createRecipe(
                "Japanese Chicken Teriyaki Bowl",
                "Pan-seared tender chicken thighs glazed in a sticky homemade teriyaki sauce, served over fluffy steamed rice with steamed broccoli.",
                Recipe.Difficulty.EASY, 15, 15, 2, users.get(4),
                List.of(tagMap.get("asian"), tagMap.get("high-protein"), tagMap.get("dinner"), tagMap.get("quick")),
                "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1512058564366-18510be2db19?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Chicken Thighs", "400", getUnit(unitMap, "g")),
                        createIng("Soy Sauce", "3", getUnit(unitMap, "tbsp")),
                        createIng("Mirin", "1", getUnit(unitMap, "tbsp")),
                        createIng("Sake", "1", getUnit(unitMap, "tbsp")),
                        createIng("Honey", "1", getUnit(unitMap, "tbsp")),
                        createIng("Jasmine Rice", "2", getUnit(unitMap, "cup")),
                        createIng("Broccoli", "1", getUnit(unitMap, "cup"))
                ),
                List.of(
                        createStep(1, "Sear the chicken thighs skin-side down in a hot skillet until crispy and golden.", true, 360, null, 0),
                        createStep(2, "Pour in soy sauce, mirin, sake, and honey, simmering until sauce forms a thick sticky glaze.", true, 300, null, 1, 2, 3, 4),
                        createStep(3, "Slice the chicken, place over steamed jasmine rice with broccoli, and drizzle generously with the teriyaki sauce.", false, null, null, 5, 6)
                )
        ));

        // 4. Authentic Mexican Beef Birria Tacos
        recipeList.add(createRecipe(
                "Authentic Mexican Beef Birria Tacos",
                "Slow-braised tender shredded beef in rich guajillo-ancho chili broth, stuffed into corn tortillas with melted Oaxaca cheese and seared crispy.",
                Recipe.Difficulty.HARD, 30, 150, 4, users.get(11),
                List.of(tagMap.get("mexican"), tagMap.get("comfort-food"), tagMap.get("dinner"), tagMap.get("spicy")),
                "https://images.unsplash.com/photo-1565299585323-38d6b0865b47?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1551504734-5ee1c4a1479b?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Beef Chuck", "1", getUnit(unitMap, "kg")),
                        createIng("Guajillo Chilies", "3", getUnit(unitMap, "piece")),
                        createIng("Ancho Chilies", "3", getUnit(unitMap, "piece")),
                        createIng("Beef Broth", "1", getUnit(unitMap, "l")),
                        createIng("Corn Tortillas", "12", getUnit(unitMap, "piece")),
                        createIng("Oaxaca Cheese", "200", getUnit(unitMap, "g"))
                ),
                List.of(
                        createStep(1, "Rehydrate the dried guajillo chilies and ancho chilies, blend with garlic and spices, and marinate the beef chuck.", false, null, null, 1, 2, 0),
                        createStep(2, "Slow braise the beef chuck in beef broth until melt-in-your-mouth tender, then shred finely.", true, 7200, null, 3),
                        createStep(3, "Dip the corn tortillas into consommé, fill with oaxaca cheese and shredded beef, fold and fry till crispy.", true, 300, null, 4, 5)
                )
        ));

        // 5. Creamy Tuscan Garlic Chicken
        recipeList.add(createRecipe(
                "Creamy Tuscan Garlic Chicken",
                "Golden pan-seared chicken breasts simmered in a velvety garlic cream sauce enriched with sun-dried tomatoes and fresh spinach.",
                Recipe.Difficulty.MEDIUM, 15, 20, 4, users.get(0),
                List.of(tagMap.get("italian"), tagMap.get("high-protein"), tagMap.get("dinner"), tagMap.get("gluten-free")),
                "https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1604908176997-125f25cc6f3d?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Chicken Breast Cutlets", "500", getUnit(unitMap, "g")),
                        createIng("Heavy Cream", "1", getUnit(unitMap, "cup")),
                        createIng("Sun-Dried Tomatoes", "0.5", getUnit(unitMap, "cup")),
                        createIng("Fresh Spinach", "2", getUnit(unitMap, "cup")),
                        createIng("Garlic Cloves", "4", getUnit(unitMap, "clove"))
                ),
                List.of(
                        createStep(1, "Season and pan-sear chicken breast cutlets until golden on both sides.", true, 480, null, 0),
                        createStep(2, "Sauté the garlic cloves and sun-dried tomatoes, then pour in the heavy cream and simmer.", true, 300, null, 1, 2, 4),
                        createStep(3, "Stir in fresh spinach until wilted, return chicken to pan, and spoon sauce over top.", true, 180, null, 3)
                )
        ));

        // 6. Fresh Greek Salad with Feta & Olives
        recipeList.add(createRecipe(
                "Fresh Greek Salad with Feta & Olives",
                "Crisp cucumbers, ripe vine tomatoes, red onion, Kalamata olives, and a slab of creamy Greek feta tossed in extra virgin olive oil and oregano.",
                Recipe.Difficulty.EASY, 15, 0, 2, users.get(3),
                List.of(tagMap.get("salad"), tagMap.get("healthy"), tagMap.get("vegetarian"), tagMap.get("quick")),
                "https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Tomatoes", "3", getUnit(unitMap, "piece")),
                        createIng("Cucumber", "1", getUnit(unitMap, "piece")),
                        createIng("Kalamata Olives", "0.5", getUnit(unitMap, "cup")),
                        createIng("Feta Cheese", "150", getUnit(unitMap, "g")),
                        createIng("Olive Oil", "2", getUnit(unitMap, "tbsp")),
                        createIng("Oregano", "1", getUnit(unitMap, "tbsp"))
                ),
                List.of(
                        createStep(1, "Chop tomatoes and cucumber into thick bitesize chunks and slice red onion thinly.", false, null, null, 0, 1),
                        createStep(2, "Combine the vegetables and kalamata olives in a bowl, drizzle generously with olive oil and wild oregano.", false, null, null, 2, 4, 5),
                        createStep(3, "Top with a full block of feta cheese and serve chilled with crusty bread.", false, null, null, 3)
                )
        ));

        // 7. Japanese Matcha Soufflé Pancakes
        recipeList.add(createRecipe(
                "Japanese Matcha Soufflé Pancakes",
                "Ultra tall, airy, pillowy soufflé pancakes infused with premium Uji matcha powder, served with whipped cream and maple syrup.",
                Recipe.Difficulty.HARD, 20, 15, 2, users.get(5),
                List.of(tagMap.get("breakfast"), tagMap.get("dessert"), tagMap.get("asian"), tagMap.get("baking")),
                "https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1567620905732-2d1ec7ab7445?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Egg Whites", "3", getUnit(unitMap, "piece")),
                        createIng("Egg Yolks", "2", getUnit(unitMap, "piece")),
                        createIng("Cake Flour", "40", getUnit(unitMap, "g")),
                        createIng("Matcha Powder", "1", getUnit(unitMap, "tbsp")),
                        createIng("Sugar", "30", getUnit(unitMap, "g"))
                ),
                List.of(
                        createStep(1, "Whisk the egg yolks, milk, cake flour, and matcha powder together into a smooth batter paste.", false, null, null, 1, 2, 3),
                        createStep(2, "Whip egg whites with sugar into stiff, glossy meringue peaks.", true, 300, null, 0, 4),
                        createStep(3, "Fold meringue gently into batter, scoop high onto non-stick pan, cover and cook on low heat.", true, 480, null)
                )
        ));

        // 8. Classic French Onion Soup
        recipeList.add(createRecipe(
                "Classic French Onion Soup",
                "Deeply caramelized yellow onions simmered in beef broth and wine, topped with toasted baguette slices and melted Gruyère cheese.",
                Recipe.Difficulty.MEDIUM, 20, 50, 4, users.get(2),
                List.of(tagMap.get("french"), tagMap.get("soup"), tagMap.get("comfort-food"), tagMap.get("dinner")),
                "https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Onions", "1", getUnit(unitMap, "kg")),
                        createIng("Beef Stock", "1.5", getUnit(unitMap, "l")),
                        createIng("White Wine", "1", getUnit(unitMap, "cup")),
                        createIng("Baguette", "4", getUnit(unitMap, "slice")),
                        createIng("Gruyère", "150", getUnit(unitMap, "g"))
                ),
                List.of(
                        createStep(1, "Slowly caramelize sliced onions in butter until dark golden brown and sweet.", true, 2100, null, 0),
                        createStep(2, "Deglaze pot with white wine, add beef stock, and simmer gently.", true, 1200, null, 1, 2),
                        createStep(3, "Ladle soup into oven-safe ramekins, top with toasted baguette and Gruyère, broil until bubbling.", true, 300, null, 3, 4)
                )
        ));

        // 9. Gourmet Avocado Toast with Poached Egg
        recipeList.add(createRecipe(
                "Gourmet Avocado Toast with Poached Egg",
                "Artisan toasted sourdough spread with smashed lemon avocado, topped with a runny poached egg, radishes, and red pepper flakes.",
                Recipe.Difficulty.EASY, 10, 5, 1, users.get(3),
                List.of(tagMap.get("breakfast"), tagMap.get("healthy"), tagMap.get("quick"), tagMap.get("vegetarian")),
                "https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Sourdough", "2", getUnit(unitMap, "slice")),
                        createIng("Avocado", "1", getUnit(unitMap, "piece")),
                        createIng("Eggs", "2", getUnit(unitMap, "piece")),
                        createIng("Lemon Juice", "1", getUnit(unitMap, "tbsp")),
                        createIng("Chili Flakes", "1", getUnit(unitMap, "tsp")),
                        createIng("Everything Seasoning", "1", getUnit(unitMap, "tsp"))
                ),
                List.of(
                        createStep(1, "Toast sourdough slices until golden and crispy.", true, 120, null, 0),
                        createStep(2, "Mash ripe avocado with lemon juice, salt, and black pepper.", false, null, null, 1, 3),
                        createStep(3, "Poach eggs in simmering vinegar water, spread avocado on toast, top with eggs, chili flakes, and everything seasoning.", true, 180, null, 2, 4, 5)
                )
        ));

        // 10. Crispy Lemon Garlic Roasted Salmon
        recipeList.add(createRecipe(
                "Crispy Lemon Garlic Roasted Salmon",
                "Oven-roasted salmon fillets with a golden garlic butter crust, fresh dill, and roasted asparagus spears.",
                Recipe.Difficulty.EASY, 10, 15, 2, users.get(7),
                List.of(tagMap.get("seafood"), tagMap.get("healthy"), tagMap.get("high-protein"), tagMap.get("gluten-free")),
                "https://images.unsplash.com/photo-1467003909585-2f8a72700288?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1467003909585-2f8a72700288?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Salmon Fillets", "400", getUnit(unitMap, "g")),
                        createIng("Butter", "2", getUnit(unitMap, "tbsp")),
                        createIng("Garlic", "3", getUnit(unitMap, "clove")),
                        createIng("Lemon Slices", "1", getUnit(unitMap, "piece")),
                        createIng("Asparagus", "1", getUnit(unitMap, "bundle"))
                ),
                List.of(
                        createStep(1, "Arrange salmon fillets and asparagus on a parchment-lined baking sheet.", false, null, null, 0, 4),
                        createStep(2, "Brush salmon with garlic butter, season with salt, pepper, and lemon slices.", false, null, null, 1, 2, 3),
                        createStep(3, "Bake in preheated 200°C oven until salmon easily flakes with a fork.", true, 720, null)
                )
        ));

        // 11. Authentic Thai Green Chicken Curry
        recipeList.add(createRecipe(
                "Authentic Thai Green Chicken Curry",
                "A fragrant coconut milk curry with tender chicken strips, Thai eggplant, bamboo shoots, and fresh sweet Thai basil leaves.",
                Recipe.Difficulty.MEDIUM, 20, 20, 4, users.get(4),
                List.of(tagMap.get("asian"), tagMap.get("spicy"), tagMap.get("dinner"), tagMap.get("high-protein")),
                "https://images.unsplash.com/photo-1455619452474-d2be8b1e70cd?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1455619452474-d2be8b1e70cd?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Green Curry Paste", "3", getUnit(unitMap, "tbsp")),
                        createIng("Coconut Milk", "400", getUnit(unitMap, "ml")),
                        createIng("Chicken Breast Strips", "500", getUnit(unitMap, "g")),
                        createIng("Thai Eggplant", "1", getUnit(unitMap, "cup")),
                        createIng("Fish Sauce", "2", getUnit(unitMap, "tsp")),
                        createIng("Palm Sugar", "1", getUnit(unitMap, "tsp"))
                ),
                List.of(
                        createStep(1, "Fry the green curry paste in a thick layer of coconut milk until aromatic oil separates.", true, 180, null, 0, 1),
                        createStep(2, "Add the chicken breast strips and cook until the outer surface turns opaque.", true, 300, null, 2),
                        createStep(3, "Pour in the remaining coconut milk, thai eggplant, fish sauce, and palm sugar, simmer, and finish with Thai basil.", true, 600, null, 3, 4, 5)
                )
        ));

        // 12. Classic French Beef Bourguignon
        recipeList.add(createRecipe(
                "Classic French Beef Bourguignon",
                "Julia Child's iconic French stew featuring tender beef chuck braised slow in red Burgundy wine with pearl onions, bacon lardons, and mushrooms.",
                Recipe.Difficulty.HARD, 35, 180, 6, users.get(2),
                List.of(tagMap.get("french"), tagMap.get("dinner"), tagMap.get("comfort-food"), tagMap.get("high-protein")),
                "https://images.unsplash.com/photo-1534939561126-855b8675edd7?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1534939561126-855b8675edd7?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Beef Chuck", "1.2", getUnit(unitMap, "kg")),
                        createIng("Red Wine", "750", getUnit(unitMap, "ml")),
                        createIng("Bacon Lardons", "150", getUnit(unitMap, "g")),
                        createIng("Pearl Onions", "200", getUnit(unitMap, "g")),
                        createIng("Cremini Mushrooms", "250", getUnit(unitMap, "g"))
                ),
                List.of(
                        createStep(1, "Sauté the bacon lardons until crispy, sear the seasoned beef chuck cubes in bacon fat until deep brown.", true, 600, null, 0, 2),
                        createStep(2, "Pour in red wine and beef broth, cover pot, and transfer to oven at 160°C to braise.", true, 9000, null, 1),
                        createStep(3, "Sauté the pearl onions and cremini mushrooms separately in butter, stir into the beef stew for the final 20 minutes.", true, 1200, null, 3, 4)
                )
        ));

        // 13. Berry Acai Smoothie Bowl
        recipeList.add(createRecipe(
                "Berry Acai Smoothie Bowl",
                "Thick vibrant frozen acai and wild berry blend topped with chia seeds, sliced bananas, toasted coconut flakes, and crunchy granola.",
                Recipe.Difficulty.EASY, 10, 0, 1, users.get(3),
                List.of(tagMap.get("breakfast"), tagMap.get("vegan"), tagMap.get("healthy"), tagMap.get("quick")),
                "https://images.unsplash.com/photo-1590301157890-4810ed352733?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1590301157890-4810ed352733?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Frozen Acai", "100", getUnit(unitMap, "g")),
                        createIng("Mixed Berries", "1", getUnit(unitMap, "cup")),
                        createIng("Almond Milk", "0.5", getUnit(unitMap, "cup")),
                        createIng("Banana", "1", getUnit(unitMap, "piece")),
                        createIng("Granola", "2", getUnit(unitMap, "tbsp")),
                        createIng("Chia Seeds", "1", getUnit(unitMap, "tbsp"))
                ),
                List.of(
                        createStep(1, "Blend frozen acai, mixed berries, and almond milk on high speed until thick and creamy.", false, null, null, 0, 1, 2),
                        createStep(2, "Scoop thick smoothie into a wide chilled bowl.", false, null, null),
                        createStep(3, "Arrange banana slices, granola, and chia seeds neatly in rows across the top.", false, null, null, 3, 4, 5)
                )
        ));

        // 14. Gourmet Double Cheeseburger with Secret Sauce
        recipeList.add(createRecipe(
                "Gourmet Double Cheeseburger with Secret Sauce",
                "Two crispy smashed beef patties, melted American cheese, caramelized onions, pickles, and tangy homemade burger sauce on toasted brioche.",
                Recipe.Difficulty.EASY, 15, 10, 1, users.get(9),
                List.of(tagMap.get("comfort-food"), tagMap.get("dinner"), tagMap.get("high-protein")),
                "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1568901346375-23c9450c58cd?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Ground Beef", "200", getUnit(unitMap, "g")),
                        createIng("American Cheese", "2", getUnit(unitMap, "slice")),
                        createIng("Brioche", "1", getUnit(unitMap, "piece")),
                        createIng("Pickles", "4", getUnit(unitMap, "slice")),
                        createIng("Secret Sauce", "2", getUnit(unitMap, "tbsp"))
                ),
                List.of(
                        createStep(1, "Divide ground beef into two balls and smash flat onto a scorching hot cast iron pan.", true, 120, null, 0),
                        createStep(2, "Flip the patties, place american cheese on top, and let melt until the crust is dark and the cheese is gooey.", true, 60, null, 1),
                        createStep(3, "Spread the secret sauce on butter-toasted brioche, add the patties and pickles, and serve immediately.", false, null, null, 2, 3, 4)
                )
        ));

        // 15. Creamy Wild Mushroom Risotto
        recipeList.add(createRecipe(
                "Creamy Wild Mushroom Risotto",
                "Slow-stirred Italian Arborio rice cooked in savory vegetable stock, finished with sautéed porcini mushrooms, butter, and Parmigiano-Reggiano.",
                Recipe.Difficulty.MEDIUM, 15, 30, 4, users.get(14),
                List.of(tagMap.get("italian"), tagMap.get("vegetarian"), tagMap.get("gluten-free"), tagMap.get("dinner")),
                "https://images.unsplash.com/photo-1633964913295-ceb43826e7c9?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1633964913295-ceb43826e7c9?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Arborio Rice", "300", getUnit(unitMap, "g")),
                        createIng("Wild Mushrooms", "300", getUnit(unitMap, "g")),
                        createIng("Vegetable Stock", "1", getUnit(unitMap, "l")),
                        createIng("White Wine", "0.5", getUnit(unitMap, "cup")),
                        createIng("Parmigiano-Reggiano", "80", getUnit(unitMap, "g"))
                ),
                List.of(
                        createStep(1, "Sauté sliced wild mushrooms in olive oil until golden, set half aside for garnish.", true, 300, null, 1),
                        createStep(2, "Toast the Arborio rice in shallot oil, deglaze with white wine, then add the vegetable stock ladle by ladle while stirring.", true, 1200, null, 0, 2, 3),
                        createStep(3, "Remove from heat, stir in butter, parmigiano-reggiano, and the mushroom blend until glossy.", false, null, null, 4)
                )
        ));

        // 16. Spanish Seafood Paella
        recipeList.add(createRecipe(
                "Spanish Seafood Paella",
                "Traditional Valencian saffron rice studded with jumbo shrimp, mussels, calamari rings, red bell peppers, and peas.",
                Recipe.Difficulty.HARD, 25, 35, 6, users.get(7),
                List.of(tagMap.get("seafood"), tagMap.get("dinner"), tagMap.get("gluten-free")),
                "https://images.unsplash.com/photo-1534080564583-6be75777b70a?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1534080564583-6be75777b70a?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Paella Rice", "400", getUnit(unitMap, "g")),
                        createIng("Shrimp", "300", getUnit(unitMap, "g")),
                        createIng("Mussels", "300", getUnit(unitMap, "g")),
                        createIng("Saffron Threads", "1", getUnit(unitMap, "pinch")),
                        createIng("Fish Stock", "1", getUnit(unitMap, "l")),
                        createIng("Smoked Paprika", "1", getUnit(unitMap, "tsp"))
                ),
                List.of(
                        createStep(1, "Infuse the warm fish stock with saffron threads and smoked paprika.", false, null, null, 3, 4, 5),
                        createStep(2, "Build sofrito base in paella pan, add the paella rice, stir to coat, and pour in saffron broth without stirring.", true, 900, null, 0),
                        createStep(3, "Nestle the shrimp and mussels into the rice during the last 10 minutes to form the crispy bottom socarrat crust.", true, 600, null, 1, 2)
                )
        ));

        // 17. Decadent Chocolate Molten Lava Cake
        recipeList.add(createRecipe(
                "Decadent Chocolate Molten Lava Cake",
                "Rich dark chocolate cakes baked with a warm, gooey liquid chocolate center, dusted with powdered sugar and vanilla ice cream.",
                Recipe.Difficulty.MEDIUM, 15, 12, 2, users.get(5),
                List.of(tagMap.get("dessert"), tagMap.get("baking"), tagMap.get("comfort-food")),
                "https://images.unsplash.com/photo-1606313564200-e75d5e30476c?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1606313564200-e75d5e30476c?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Dark Chocolate", "120", getUnit(unitMap, "g")),
                        createIng("Butter", "100", getUnit(unitMap, "g")),
                        createIng("Eggs", "4", getUnit(unitMap, "piece")),
                        createIng("Powdered Sugar", "50", getUnit(unitMap, "g")),
                        createIng("All-Purpose Flour", "30", getUnit(unitMap, "g"))
                ),
                List.of(
                        createStep(1, "Melt dark chocolate and butter over a water bath until smooth.", false, null, null, 0, 1),
                        createStep(2, "Whisk the eggs and powdered sugar until thick and pale, then gently fold in the chocolate mixture and all-purpose flour.", false, null, null, 2, 3, 4),
                        createStep(3, "Pour into greased ramekins and bake at 210°C for 12 minutes until edges are set but center soft.", true, 720, null)
                )
        ));

        // 18. Crispy Falafel Pita Pocket with Tahini
        recipeList.add(createRecipe(
                "Crispy Falafel Pita Pocket with Tahini",
                "Golden crispy chickpea falafel stuffed into warm fluffy pita bread with Israeli diced salad, pickles, and rich garlic tahini drizzle.",
                Recipe.Difficulty.MEDIUM, 25, 15, 3, users.get(13),
                List.of(tagMap.get("middle-eastern"), tagMap.get("vegan"), tagMap.get("healthy")),
                "https://images.unsplash.com/photo-1593001874117-c99c800e3eb7?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1593001874117-c99c800e3eb7?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Chickpeas", "250", getUnit(unitMap, "g")),
                        createIng("Parsley", "0.5", getUnit(unitMap, "cup")),
                        createIng("Cilantro", "0.5", getUnit(unitMap, "cup")),
                        createIng("Tahini", "0.5", getUnit(unitMap, "cup")),
                        createIng("Pita Breads", "3", getUnit(unitMap, "piece")),
                        createIng("Israeli Salad", "1.5", getUnit(unitMap, "cup"))
                ),
                List.of(
                        createStep(1, "Pulse the soaked chickpeas, parsley, cilantro, garlic, and spices in a food processor until a coarse meal forms.", false, null, null, 0, 1, 2),
                        createStep(2, "Form falafel balls and deep fry in hot oil until deep golden brown and crispy.", true, 300, null),
                        createStep(3, "Whisk the tahini with lemon juice and cold water, stuff the pita breads with falafel and israeli salad, and drizzle with the tahini.", false, null, null, 3, 4, 5)
                )
        ));

        // 19. Traditional Vietnamese Beef Pho
        recipeList.add(createRecipe(
                "Traditional Vietnamese Beef Pho",
                "A aromatic 12-hour spiced beef bone broth poured over rice noodles, thinly sliced raw eye round beef, fresh basil, and bean sprouts.",
                Recipe.Difficulty.HARD, 30, 240, 4, users.get(4),
                List.of(tagMap.get("asian"), tagMap.get("soup"), tagMap.get("comfort-food"), tagMap.get("high-protein")),
                "https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1582878826629-29b7ad1cdc43?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Beef Marrow Bones", "1.5", getUnit(unitMap, "kg")),
                        createIng("Star Anise", "2", getUnit(unitMap, "piece")),
                        createIng("Cinnamon", "1", getUnit(unitMap, "piece")),
                        createIng("Ginger", "1", getUnit(unitMap, "piece")),
                        createIng("Rice Pho Noodles", "400", getUnit(unitMap, "g")),
                        createIng("Eye Round Beef", "300", getUnit(unitMap, "g")),
                        createIng("Thai Basil", "1", getUnit(unitMap, "handful")),
                        createIng("Bean Sprouts", "1", getUnit(unitMap, "handful"))
                ),
                List.of(
                        createStep(1, "Char the ginger and onions, simmer the beef marrow bones with the charred aromatics, star anise, and cinnamon in a spice bag.", true, 14400, null, 0, 1, 2, 3),
                        createStep(2, "Blanch the rice pho noodles in boiling water, divide into serving bowls.", true, 120, null, 4),
                        createStep(3, "Top the noodles with raw eye round beef slices, ladle the boiling hot broth over to cook the beef instantly, serve with thai basil and bean sprouts.", false, null, null, 5, 6, 7)
                )
        ));

        // 20. Classic Chicken Caesar Salad
        recipeList.add(createRecipe(
                "Classic Chicken Caesar Salad",
                "Crisp Romaine lettuce hearts tossed in creamy anchovy-garlic Caesar dressing, crunchy garlic sourdough croutons, shaved Parmesan, and grilled chicken.",
                Recipe.Difficulty.EASY, 15, 10, 2, users.get(10),
                List.of(tagMap.get("salad"), tagMap.get("high-protein"), tagMap.get("healthy")),
                "https://images.unsplash.com/photo-1550304943-4f24f54ddde9?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1550304943-4f24f54ddde9?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Romaine Lettuce", "2", getUnit(unitMap, "piece")),
                        createIng("Chicken Breast", "300", getUnit(unitMap, "g")),
                        createIng("Sourdough Garlic Croutons", "1", getUnit(unitMap, "cup")),
                        createIng("Shaved Parmesan Cheese", "50", getUnit(unitMap, "g")),
                        createIng("Caesar Dressing", "4", getUnit(unitMap, "tbsp"))
                ),
                List.of(
                        createStep(1, "Grill the seasoned chicken breast, then slice into diagonal strips.", true, 480, null, 1),
                        createStep(2, "Chop the romaine lettuce and toss with homemade Caesar dressing in a large wooden bowl.", false, null, null, 0, 4),
                        createStep(3, "Top with the sliced chicken strips, crunchy sourdough garlic croutons, and shaved Parmesan cheese.", false, null, null, 2, 3)
                )
        ));

        // 21. Authentic Indian Butter Chicken (Murgh Makhani)
        recipeList.add(createRecipe(
                "Authentic Indian Butter Chicken",
                "Tender spiced yogurt-marinated chicken pieces simmered in a rich tomato, cream, butter, and garam masala sauce with warm naan bread.",
                Recipe.Difficulty.MEDIUM, 25, 25, 4, users.get(6),
                List.of(tagMap.get("asian"), tagMap.get("comfort-food"), tagMap.get("dinner"), tagMap.get("high-protein")),
                "https://images.unsplash.com/photo-1588166524941-3bf61a9c41db?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1588166524941-3bf61a9c41db?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Chicken Thighs", "600", getUnit(unitMap, "g")),
                        createIng("Tomato Puree", "1.5", getUnit(unitMap, "cup")),
                        createIng("Heavy Cream", "0.25", getUnit(unitMap, "cup")),
                        createIng("Butter", "2", getUnit(unitMap, "tbsp")),
                        createIng("Garam Masala", "1", getUnit(unitMap, "tbsp")),
                        createIng("Fenugreek Leaves", "1", getUnit(unitMap, "tsp")),
                        createIng("Naan", "4", getUnit(unitMap, "piece"))
                ),
                List.of(
                        createStep(1, "Sear the marinated chicken thighs in a hot skillet or broiler until lightly charred.", true, 420, null, 0),
                        createStep(2, "Simmer the tomato puree with butter, heavy cream, garam masala, and fenugreek leaves until silky smooth.", true, 600, null, 1, 2, 3, 4, 5),
                        createStep(3, "Combine the chicken into the sauce, simmer for 5 minutes, garnish with cream, and serve with warm naan.", true, 300, null, 6)
                )
        ));

        // 22. Mediterranean Grilled Chicken Souvlaki
        recipeList.add(createRecipe(
                "Mediterranean Grilled Chicken Souvlaki",
                "Skewered tender lemon-herb marinated chicken grilled over flames, served with cool cucumber tzatziki sauce and fluffy pita.",
                Recipe.Difficulty.EASY, 20, 15, 3, users.get(0),
                List.of(tagMap.get("middle-eastern"), tagMap.get("high-protein"), tagMap.get("healthy")),
                "https://images.unsplash.com/photo-1529193591184-b1d58069ecdd?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1529193591184-b1d58069ecdd?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Chicken Cubes", "500", getUnit(unitMap, "g")),
                        createIng("Lemon Juice", "2", getUnit(unitMap, "tbsp")),
                        createIng("Olive Oil", "2", getUnit(unitMap, "tbsp")),
                        createIng("Oregano", "1", getUnit(unitMap, "tbsp")),
                        createIng("Garlic", "3", getUnit(unitMap, "clove")),
                        createIng("Tzatziki Dip", "1", getUnit(unitMap, "cup")),
                        createIng("Pita", "3", getUnit(unitMap, "piece"))
                ),
                List.of(
                        createStep(1, "Marinate the chicken cubes in olive oil, lemon juice, garlic, and oregano for 30 minutes.", false, null, null, 0, 1, 2, 3, 4),
                        createStep(2, "Thread chicken onto wooden skewers and grill over high heat until charred and tender.", true, 600, null),
                        createStep(3, "Serve the skewers hot off the grill alongside cold tzatziki dip and warmed pita bread.", false, null, null, 5, 6)
                )
        ));

        // 23. Authentic Italian Margherita Pizza
        recipeList.add(createRecipe(
                "Authentic Italian Margherita Pizza",
                "Classic Neapolitan thin-crust pizza topped with San Marzano tomato sauce, fresh mozzarella di bufala, and fragrant sweet basil leaves.",
                Recipe.Difficulty.MEDIUM, 30, 10, 2, users.get(14),
                List.of(tagMap.get("italian"), tagMap.get("baking"), tagMap.get("vegetarian"), tagMap.get("dinner")),
                "https://images.unsplash.com/photo-1574071318508-1cdbab80d002?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1574071318508-1cdbab80d002?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Pizza Dough", "250", getUnit(unitMap, "g")),
                        createIng("Crushed San Marzano Tomatoes", "0.5", getUnit(unitMap, "cup")),
                        createIng("Mozzarella", "125", getUnit(unitMap, "g")),
                        createIng("Fresh Basil", "6", getUnit(unitMap, "piece")),
                        createIng("Extra Virgin Olive Oil", "1", getUnit(unitMap, "tbsp"))
                ),
                List.of(
                        createStep(1, "Stretch the pizza dough by hand into a thin 12-inch disc with a raised outer crust rim.", false, null, null, 0),
                        createStep(2, "Spread crushed San Marzano tomatoes lightly, add torn fresh mozzarella pieces.", false, null, null, 1, 2),
                        createStep(3, "Bake on a preheated pizza stone at 250°C (or wood-fired oven) until the crust is blistered, then finish with fresh basil and a drizzle of extra virgin olive oil.", true, 480, null, 3, 4)
                )
        ));

        // 24. Crispy Tofu Buddha Bowl with Peanut Dressing
        recipeList.add(createRecipe(
                "Crispy Tofu Buddha Bowl with Peanut Dressing",
                "Pan-crisped sesame tofu cubes served over quinoa with purple cabbage, edamame, shredded carrots, and creamy peanut ginger sauce.",
                Recipe.Difficulty.EASY, 15, 15, 2, users.get(3),
                List.of(tagMap.get("vegan"), tagMap.get("healthy"), tagMap.get("asian"), tagMap.get("high-protein")),
                "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Tofu", "350", getUnit(unitMap, "g")),
                        createIng("Quinoa", "1.5", getUnit(unitMap, "cup")),
                        createIng("Edamame", "0.5", getUnit(unitMap, "cup")),
                        createIng("Carrots", "0.5", getUnit(unitMap, "cup")),
                        createIng("Peanut Butter", "3", getUnit(unitMap, "tbsp")),
                        createIng("Soy Sauce", "1", getUnit(unitMap, "tbsp")),
                        createIng("Maple Syrup", "1", getUnit(unitMap, "tbsp")),
                        createIng("Ginger", "1", getUnit(unitMap, "tsp"))
                ),
                List.of(
                        createStep(1, "Toss tofu cubes with cornstarch and sesame oil, pan fry until crispy on all edges.", true, 480, null, 0),
                        createStep(2, "Whisk peanut butter, soy sauce, maple syrup, grated ginger, and warm water into a smooth dressing.", false, null, null, 4, 5, 6, 7),
                        createStep(3, "Assemble quinoa bowl with edamame, carrots, crispy tofu, and drizzle generously with peanut sauce.", false, null, null, 1, 2, 3)
                )
        ));

        // 25. Savoyard Potato Tartiflette
        recipeList.add(createRecipe(
                "Savoyard Potato Tartiflette",
                "A decadent French Alpine casserole of sliced potatoes, smoky bacon lardons, and caramelized onions smothered in melted Reblochon cheese.",
                Recipe.Difficulty.MEDIUM, 20, 40, 4, users.get(2),
                List.of(tagMap.get("french"), tagMap.get("comfort-food"), tagMap.get("dinner")),
                "https://images.unsplash.com/photo-1518779578993-ec3579fee39f?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1518779578993-ec3579fee39f?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Potatoes", "1", getUnit(unitMap, "kg")),
                        createIng("Bacon Lardons", "200", getUnit(unitMap, "g")),
                        createIng("Onions", "2", getUnit(unitMap, "piece")),
                        createIng("White Wine", "0.5", getUnit(unitMap, "cup")),
                        createIng("Reblochon", "1", getUnit(unitMap, "piece"))
                ),
                List.of(
                        createStep(1, "Boil unpeeled potatoes until just tender, slice into thick rounds.", true, 900, null, 0),
                        createStep(2, "Fry bacon lardons and onions in butter, deglaze pan with white wine.", true, 420, null, 1, 2, 3),
                        createStep(3, "Layer potatoes and bacon mixture in baking dish, top with halved Reblochon wheel, bake at 200°C.", true, 1500, null, 4)
                )
        ));

        // 26. Homemade New York Style Cheesecake
        recipeList.add(createRecipe(
                "Homemade New York Style Cheesecake",
                "Dense, ultra-cremy baked cheesecake with a buttery Graham cracker crust, baked slowly and topped with fresh raspberry coulis.",
                Recipe.Difficulty.HARD, 30, 60, 8, users.get(5),
                List.of(tagMap.get("dessert"), tagMap.get("baking"), tagMap.get("comfort-food")),
                "https://images.unsplash.com/photo-1533134242443-d4fd215305ad?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1533134242443-d4fd215305ad?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Cream Cheese", "900", getUnit(unitMap, "g")),
                        createIng("Sugar", "200", getUnit(unitMap, "g")),
                        createIng("Sour Cream", "0.5", getUnit(unitMap, "cup")),
                        createIng("Heavy Cream", "0.5", getUnit(unitMap, "cup")),
                        createIng("Eggs", "4", getUnit(unitMap, "piece")),
                        createIng("Graham Cracker Crumbs", "1.25", getUnit(unitMap, "cup")),
                        createIng("Butter", "4", getUnit(unitMap, "tbsp"))
                ),
                List.of(
                        createStep(1, "Press the buttery Graham cracker crumbs and butter firmly into the springform pan base and pre-bake the crust.", true, 600, null, 5, 6),
                        createStep(2, "Beat cream cheese and sugar until completely smooth, mix in sour cream, heavy cream, and eggs one by one.", false, null, null, 0, 1, 2, 3, 4),
                        createStep(3, "Bake cheesecake in a water bath at 160°C, cool slowly inside turned-off oven to prevent cracking.", true, 3600, null)
                )
        ));

        // 27. Authentic Mexican Huevos Rancheros
        recipeList.add(createRecipe(
                "Authentic Mexican Huevos Rancheros",
                "Warm corn tortillas topped with fried sunny-side-up eggs, homemade roasted tomato ranchero salsa, refried beans, and avocado.",
                Recipe.Difficulty.EASY, 15, 15, 2, users.get(11),
                List.of(tagMap.get("mexican"), tagMap.get("breakfast"), tagMap.get("vegetarian"), tagMap.get("spicy")),
                "https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Corn Tortillas", "4", getUnit(unitMap, "piece")),
                        createIng("Eggs", "4", getUnit(unitMap, "piece")),
                        createIng("Ranchero Salsa", "1", getUnit(unitMap, "cup")),
                        createIng("Refried Black Beans", "1", getUnit(unitMap, "cup")),
                        createIng("Avocado", "1", getUnit(unitMap, "piece")),
                        createIng("Cotija Cheese", "30", getUnit(unitMap, "g"))
                ),
                List.of(
                        createStep(1, "Warm the corn tortillas in oil until pliable, spread the warm refried black beans over the top.", false, null, null, 0, 3),
                        createStep(2, "Fry the eggs sunny-side up in oil until the edges are crispy and the yolk is runny.", true, 180, null, 1),
                        createStep(3, "Place the eggs over the bean tortillas, spoon the warm roasted ranchero salsa over the top, and garnish with avocado and cotija cheese.", false, null, null, 2, 4, 5)
                )
        ));

        // 28. Creamy Tomato Soup & Crispy Grilled Cheese
        recipeList.add(createRecipe(
                "Creamy Tomato Soup & Crispy Grilled Cheese",
                "Rich roasted tomato basil soup served alongside a golden, buttery sourdough grilled cheese sandwich oozing with melted cheddar.",
                Recipe.Difficulty.EASY, 15, 20, 2, users.get(8),
                List.of(tagMap.get("soup"), tagMap.get("comfort-food"), tagMap.get("quick"), tagMap.get("vegetarian")),
                "https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Roasted San Marzano Tomatoes", "800", getUnit(unitMap, "g")),
                        createIng("Fresh Basil", "0.25", getUnit(unitMap, "cup")),
                        createIng("Heavy Cream", "0.25", getUnit(unitMap, "cup")),
                        createIng("Sourdough Slices", "4", getUnit(unitMap, "slice")),
                        createIng("Sharp Cheddar", "4", getUnit(unitMap, "slice")),
                        createIng("Butter", "3", getUnit(unitMap, "tbsp"))
                ),
                List.of(
                        createStep(1, "Simmer the roasted San Marzano tomatoes with garlic and herbs, then blend smooth with heavy cream and fresh basil.", true, 600, null, 0, 1, 2),
                        createStep(2, "Butter sourdough slices generously on the outside, sandwich sharp cheddar inside.", false, null, null, 3, 4, 5),
                        createStep(3, "Grill sandwich in skillet on medium-low heat until crust is deep golden and cheese is melted.", true, 360, null)
                )
        ));

        // 29. Spicy Seared Ahi Tuna Poke Bowl
        recipeList.add(createRecipe(
                "Spicy Seared Ahi Tuna Poke Bowl",
                "Sesame-crusted seared Ahi tuna over sushi rice with mango, cucumber, edamame, avocado, and spicy sriracha mayo drizzle.",
                Recipe.Difficulty.MEDIUM, 20, 5, 2, users.get(7),
                List.of(tagMap.get("seafood"), tagMap.get("asian"), tagMap.get("healthy"), tagMap.get("high-protein")),
                "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Ahi Tuna", "300", getUnit(unitMap, "g")),
                        createIng("Sesame Seeds", "2", getUnit(unitMap, "tbsp")),
                        createIng("Sushi Rice", "2", getUnit(unitMap, "cup")),
                        createIng("Mango", "0.5", getUnit(unitMap, "cup")),
                        createIng("Avocado", "0.5", getUnit(unitMap, "cup")),
                        createIng("Sriracha Mayo", "2", getUnit(unitMap, "tbsp"))
                ),
                List.of(
                        createStep(1, "Coat Ahi tuna steak in sesame seeds and sear quickly in hot skillet for 45 seconds per side.", true, 90, null, 0, 1),
                        createStep(2, "Slice seared tuna into thin ribbons against the grain.", false, null, null),
                        createStep(3, "Arrange sushi rice bowl with seared tuna, diced mango, avocado, and drizzle with spicy sriracha mayo.", false, null, null, 2, 3, 4, 5)
                )
        ));

        // 30. Cinnamon Roll French Toast Bake
        recipeList.add(createRecipe(
                "Cinnamon Roll French Toast Bake",
                "Fluffy brioche cube casserole soaked in cinnamon egg custard, baked golden, and topped with cream cheese glaze and toasted pecans.",
                Recipe.Difficulty.EASY, 20, 35, 6, users.get(5),
                List.of(tagMap.get("breakfast"), tagMap.get("dessert"), tagMap.get("baking"), tagMap.get("comfort-food")),
                "https://images.unsplash.com/photo-1484723091739-30a097e8f929?auto=format&fit=crop&w=1200&q=80",
                List.of("https://images.unsplash.com/photo-1484723091739-30a097e8f929?auto=format&fit=crop&w=1200&q=80"),
                List.of(
                        createIng("Brioche", "500", getUnit(unitMap, "g")),
                        createIng("Eggs", "6", getUnit(unitMap, "piece")),
                        createIng("Milk", "0.75", getUnit(unitMap, "cup")),
                        createIng("Heavy Cream", "0.75", getUnit(unitMap, "cup")),
                        createIng("Cinnamon", "1", getUnit(unitMap, "tbsp")),
                        createIng("Brown Sugar", "2", getUnit(unitMap, "tbsp")),
                        createIng("Cream Cheese Glaze", "0.5", getUnit(unitMap, "cup"))
                ),
                List.of(
                        createStep(1, "Arrange cubed brioche bread in a buttered 9x13 inch baking dish.", false, null, null, 0),
                        createStep(2, "Whisk the eggs, milk, heavy cream, brown sugar, and ground cinnamon; pour evenly over the brioche cubes.", false, null, null, 1, 2, 3, 4, 5),
                        createStep(3, "Bake at 180°C until puffed and golden brown, then drizzle generously with warm cream cheese glaze.", true, 2100, null, 6)
                )
        ));

        return recipeRepository.saveAll(recipeList);
    }

    /**
     * Builds an (unsaved) ingredient entity for a seeded recipe; persisted later via
     * {@code Recipe}'s cascading save in {@link #seedRecipes(List, List, List)}.
     *
     * @param name the ingredient display name
     * @param qtyStr the ingredient quantity, as a decimal string parsed into a {@link BigDecimal}
     * @param unit the resolved measurement unit, typically via {@link #getUnit(Map, String)}; a {@code null} is logged but tolerated
     * @return the built (not yet persisted) ingredient entity
     */
    private Ingredient createIng(String name, String qtyStr, Unit unit) {
        if (unit == null) {
            log.warn("Null unit provided for ingredient '{}'. Ensure unit code exists.", name);
        }
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
     * @param imgUrl an illustrative image URL to upload for this step, or {@code null} for none
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
     * @param imageUrl an illustrative image URL to upload for this step, or {@code null} for none
     * @param linkedIngredientIndices 0-based indices into the recipe's ingredient list identifying this step's ingredients
     */
    private record InstructionStepData(int stepNumber, String description, boolean hasTimer, Integer timeSeconds, String imageUrl, List<Integer> linkedIngredientIndices) {}

    /**
     * Builds, wires, and persists a complete Recipe entity: uploads its cover/description images
     * to Cloudinary, attaches its ingredients and instruction steps (resolving each step's
     * ingredient links by index into {@code ingredients}), builds its description blocks, and
     * assembles its recipe-image gallery from {@code primaryCoverUrl} and {@code extraGalleryUrls}.
     *
     * @param title the recipe's display title
     * @param description the recipe's flat description text, also used as the first description block
     * @param difficulty the recipe's skill difficulty level
     * @param prepTime preparation duration in minutes
     * @param cookTime active cooking duration in minutes
     * @param servings recommended serving yield count
     * @param creator the seeded user to set as the recipe's author
     * @param tags the seeded tag entities to associate with the recipe
     * @param primaryCoverUrl the source URL uploaded as the recipe's cover image and description image
     * @param extraGalleryUrls additional source URLs uploaded as non-primary gallery images
     * @param ingredients the recipe's ingredient entities, as built by {@link #createIng}
     * @param stepDataList the recipe's instruction steps, as built by {@link #createStep}
     * @return the persisted recipe entity
     */
    private Recipe createRecipe(String title, String description, Recipe.Difficulty difficulty,
                                int prepTime, int cookTime, int servings, User creator, List<Tag> tags,
                                String primaryCoverUrl, List<String> extraGalleryUrls,
                                List<Ingredient> ingredients, List<InstructionStepData> stepDataList) {

        String userId = creator.getId();
        String recipeTitle = title.replaceAll("[^a-zA-Z0-9_-]", "_");
        String recipeFolder = cloudinaryService.buildUserFolder(creator.getEmail(), recipeTitle);

        long currentTime = System.currentTimeMillis();

        // 7.2 Main image: cooksync/[userId]/[recipeTittle], name: main_[userId]_[currentTime]
        String mainPublicId = String.format("main_%s_%d", userId, currentTime);
        String uploadedCoverUrl = uploadToCloudinary(primaryCoverUrl, recipeFolder, mainPublicId);

        // 7.3 Description image: cooksync/[userId]/[recipeTittle], name: description_[userId]_[currentTime]
        String descPublicId = String.format("description_%s_%d", userId, currentTime);
        String uploadedDescUrl = uploadToCloudinary(primaryCoverUrl, recipeFolder, descPublicId);

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

        // Wire instructions (7.4 Instruction image: cooksync/[userId]/[recipeTittle], name: instruction_[stepNumber]_[currentTime])
        List<Ingredient> ingList = new ArrayList<>(ingredients);
        Set<Instruction> instructionSet = new LinkedHashSet<>();
        for (InstructionStepData step : stepDataList) {
            Set<Ingredient> stepIngs = new LinkedHashSet<>();
            for (Integer idx : step.linkedIngredientIndices()) {
                if (idx != null && idx >= 0 && idx < ingList.size()) {
                    stepIngs.add(ingList.get(idx));
                }
            }

            String uploadedStepImg = null;
            if (step.imageUrl() != null) {
                long stepTime = System.currentTimeMillis();
                String stepPublicId = String.format("instruction_%d_%d", step.stepNumber(), stepTime);
                uploadedStepImg = uploadToCloudinary(step.imageUrl(), recipeFolder, stepPublicId);
            }

            Instruction instruction = Instruction.builder()
                    .stepNumber(step.stepNumber())
                    .description(step.description())
                    .hasTimer(step.hasTimer())
                    .timeSeconds(step.timeSeconds())
                    .imageUrl(uploadedStepImg)
                    .ingredients(stepIngs)
                    .recipe(recipe)
                    .build();
            instructionSet.add(instruction);
        }
        recipe.setInstructions(instructionSet);

        // Build description blocks
        List<DescriptionBlock> blocks = new ArrayList<>();
        blocks.add(DescriptionBlock.builder()
                .recipe(recipe)
                .type(DescriptionBlock.BlockType.TEXT)
                .text(description)
                .sortOrder(0)
                .build());
        blocks.add(DescriptionBlock.builder()
                .recipe(recipe)
                .type(DescriptionBlock.BlockType.IMAGE)
                .imageUrl(uploadedDescUrl)
                .caption(title + " presentation")
                .sortOrder(1)
                .build());
        blocks.add(DescriptionBlock.builder()
                .recipe(recipe)
                .type(DescriptionBlock.BlockType.TEXT)
                .text("Follow the detailed instructions below for the best culinary experience. Enjoy cooking!")
                .sortOrder(2)
                .build());
        recipe.setDescriptionBlocks(blocks);

        // Build recipe images
        Set<RecipeImage> recipeImages = new LinkedHashSet<>();
        recipeImages.add(RecipeImage.builder()
                .recipe(recipe)
                .imageUrl(uploadedCoverUrl)
                .isPrimary(true)
                .build());

        if (extraGalleryUrls != null) {
            for (int i = 0; i < extraGalleryUrls.size(); i++) {
                long galleryTime = System.currentTimeMillis();
                String galleryPublicId = String.format("gallery_%d_%d", i + 1, galleryTime);
                String uploadedExtra = uploadToCloudinary(extraGalleryUrls.get(i), recipeFolder, galleryPublicId);
                recipeImages.add(RecipeImage.builder()
                        .recipe(recipe)
                        .imageUrl(uploadedExtra)
                        .isPrimary(false)
                        .build());
            }
        }
        recipe.setImages(recipeImages);

        return recipe;
    }

    /** 
     * Seeds a varied, mostly-positive spread of reviews across every recipe (skewed 50% 5-star,
     * 40% 4-star, 10% 3-star, as a real recipe app's published catalog would trend), plus two
     * deliberately reported reviews to exercise the admin moderation queue. Recomputes each
     * recipe's denormalized rating stats via {@link #recalculateRecipeStats(List, List)} once
     * all reviews are saved.
     *
     * @param recipes the seeded recipes to attach reviews to
     * @param users the seeded users to attribute reviews to
     */
    private void seedReviews(List<Recipe> recipes, List<User> users) {
        log.info(">>> Seeding user reviews and ratings...");
        List<Review> reviews = new ArrayList<>();

        String[] fiveStarComments = {
                "Absolutely incredible recipe! The flavors were so rich and perfectly balanced.",
                "Tried this for dinner tonight and it turned out restaurant quality!",
                "The sauce turned out so glossy and rich. Saved to my favorites!",
                "Made this three times already this month, my family can't get enough.",
                "Followed it exactly and it came out perfect on the first try.",
                "This is now in permanent weekly rotation at our house.",
                "Better than the version I had at the restaurant that inspired it."
        };
        String[] fiveStarTitles = {"Outstanding dish!", "New family favorite", "Cooking this again for sure", "Nailed it first try"};

        String[] fourStarComments = {
                "Easy to follow steps! My family wiped out the entire plate in minutes.",
                "Great instructions and perfect timer recommendations. Will definitely make again.",
                "Substituted one spice and it was still phenomenal. 10/10 recommend!",
                "Super fresh and satisfying! Perfect for weeknight meal prep.",
                "Really solid recipe, just needed a bit more salt for my taste.",
                "Turned out great, though it took a little longer than the listed time.",
                "Delicious! I'll cut back on the spice next time for the kids."
        };
        String[] fourStarTitles = {"Really great recipe", "Would make again", "Tasty and easy", "Solid recipe"};

        String[] threeStarComments = {
                "Good base recipe, but I had to tweak the seasoning quite a bit.",
                "Came out a little dry for me - might reduce the cook time slightly next time.",
                "Tasty, though not quite as impressive as the photos suggested.",
                "Solid weeknight option, nothing fancy but reliable."
        };
        String[] threeStarTitles = {"Decent, with tweaks", "Good but needs adjusting", "Worth trying"};

        for (int i = 0; i < recipes.size(); i++) {
            Recipe recipe = recipes.get(i);
            int reviewCount = 3 + (i % 5);

            for (int r = 0; r < reviewCount; r++) {
                User reviewer = users.get((i + r + 1) % users.size());
                // Roughly half 5-star, 40% 4-star, 10% 3-star - varied but still skewed positive,
                // like a real recipe app where poorly-received recipes rarely stay published.
                int cycle = (i + r) % 10;
                int ratingVal = cycle < 5 ? 5 : (cycle < 9 ? 4 : 3);
                String title;
                String comment;
                switch (ratingVal) {
                    case 5 -> {
                        title = fiveStarTitles[(i + r) % fiveStarTitles.length];
                        comment = fiveStarComments[(i + r) % fiveStarComments.length];
                    }
                    case 4 -> {
                        title = fourStarTitles[(i + r) % fourStarTitles.length];
                        comment = fourStarComments[(i + r) % fourStarComments.length];
                    }
                    default -> {
                        title = threeStarTitles[(i + r) % threeStarTitles.length];
                        comment = threeStarComments[(i + r) % threeStarComments.length];
                    }
                }

                reviews.add(Review.builder()
                        .recipe(recipe)
                        .user(reviewer)
                        .rating(BigDecimal.valueOf(ratingVal))
                        .title(title)
                        .comment(comment)
                        .build());
            }
        }

        // Add reported test reviews for moderation features
        if (!recipes.isEmpty() && users.size() >= 4) {
            reviews.add(Review.builder()
                    .recipe(recipes.get(0))
                    .user(users.get(users.size() - 1))
                    .rating(BigDecimal.valueOf(1))
                    .title("Unrelated spam")
                    .comment("Check out cheap cookware deals at spam-link.com now!")
                    .reported(true)
                    .reportReason(Review.ReportReason.SPAM)
                    .reportedAt(LocalDateTime.now())
                    .build());

            reviews.add(Review.builder()
                    .recipe(recipes.get(1))
                    .user(users.get(users.size() - 2))
                    .rating(BigDecimal.valueOf(1))
                    .title("Inappropriate comment")
                    .comment("Rude comments violating community guidelines.")
                    .reported(true)
                    .reportReason(Review.ReportReason.ABUSE)
                    .reportedAt(LocalDateTime.now())
                    .build());
        }

        reviewRepository.saveAll(reviews);
        recalculateRecipeStats(recipes, reviews);
        recipeRepository.saveAll(recipes);
    }

    /**
     * Recomputes each seeded recipe's denormalized {@code reviewCount} and {@code averageRating}
     * from its just-seeded reviews, mirroring the aggregation the live review-creation/deletion
     * flow performs, so the seeded data is internally consistent from the start.
     *
     * @param recipes the seeded recipes to update
     * @param reviews the seeded reviews to aggregate per recipe
     */
    private void recalculateRecipeStats(List<Recipe> recipes, List<Review> reviews) {
        Map<String, List<Review>> reviewsByRecipeId = reviews.stream()
                .collect(Collectors.groupingBy(review -> review.getRecipe().getId()));

        for (Recipe recipe : recipes) {
            List<Review> recipeReviews = reviewsByRecipeId.getOrDefault(recipe.getId(), List.of());
            recipe.setReviewCount(recipeReviews.size());
            recipe.setAverageRating(recipeReviews.isEmpty() ? null
                    : recipeReviews.stream().mapToDouble(r -> r.getRating().doubleValue()).average().orElse(0.0));
        }
    }

    /**
     * Seeds one favorite bookmark per recipe, cycling through the seeded users as bookmarkers.
     *
     * @param recipes the seeded recipes to bookmark
     * @param users the seeded users to assign as bookmarkers, cycled by index
     */
    private void seedFavorites(List<Recipe> recipes, List<User> users) {
        log.info(">>> Seeding user favorites...");
        List<FavoriteRecipe> favorites = new ArrayList<>();
        for (int i = 0; i < recipes.size(); i++) {
            Recipe recipe = recipes.get(i);
            User user = users.get(i % users.size());
            favorites.add(FavoriteRecipe.builder()
                    .user(user)
                    .recipe(recipe)
                    .build());
        }
        favoriteRecipeRepository.saveAll(favorites);
    }

    /**
     * Seeds one recipe-wide personal note per recipe, cycling through a fixed pool of sample
     * note texts and through the seeded users as note authors.
     *
     * @param recipes the seeded recipes to attach a note to
     * @param users the seeded users to assign as note authors, cycled by index
     */
    private void seedPersonalNotes(List<Recipe> recipes, List<User> users) {
        log.info(">>> Seeding personal notes...");
        List<PersonalInstructionNote> notes = new ArrayList<>();

        String[] noteTemplates = {
                "Add an extra pinch of sea salt right before serving.",
                "Double the recipe next time - it goes fast!",
                "Swapped in what I had on hand and it still worked great.",
                "Let it rest a few extra minutes before serving - makes a real difference.",
                "My go-to for busy weeknights, prepped ahead on Sunday.",
                "Kids loved this one, used a bit less spice for them.",
                "Worth the extra step - don't skip it.",
                "Great for meal prep, keeps well for a few days in the fridge.",
                "Cut back slightly on the sugar and it was still perfect.",
                "Freezes surprisingly well for a quick reheat later."
        };

        for (int i = 0; i < recipes.size(); i++) {
            Recipe recipe = recipes.get(i);
            User user = users.get(i % users.size());
            notes.add(PersonalInstructionNote.builder()
                    .user(user)
                    .recipe(recipe)
                    .note(noteTemplates[i % noteTemplates.length])
                    .build());
        }
        personalInstructionNoteRepository.saveAll(notes);
    }
}
