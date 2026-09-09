package com.cooksync_server.services;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.cooksync_server.constants.EntityNames;
import com.cooksync_server.entities.RecipeImportJob;
import com.cooksync_server.entities.RecipeImportJobImage;
import com.cooksync_server.entities.Unit;
import com.cooksync_server.entities.User;
import com.cooksync_server.exceptions.RateLimitExceededException;
import com.cooksync_server.exceptions.ResourceNotFoundException;
import com.cooksync_server.mappers.RecipeImportJobMapper;
import com.cooksync_server.recipeimport.ExtractedRecipe;
import com.cooksync_server.recipeimport.ExtractionServiceException;
import com.cooksync_server.recipeimport.ImageInput;
import com.cooksync_server.recipeimport.RecipeExtractionProvider;
import com.cooksync_server.recipeimport.RobotsTxtChecker;
import com.cooksync_server.recipeimport.WebPageTextExtractor;
import com.cooksync_server.repositories.RecipeImportJobRepository;
import com.cooksync_server.repositories.UnitRepository;
import com.cooksync_server.repositories.UserRepository;
import com.dtos.request.ingredient.IngredientRequestDTO;
import com.dtos.request.instruction.InstructionRequestDTO;
import com.dtos.request.recipe.RecipeCreateRequestDTO;
import com.dtos.request.recipeimport.RecipeImportStartRequestDTO;
import com.dtos.response.recipe.DescriptionBlockDTO;
import com.dtos.response.recipe.RecipeResponse;
import com.dtos.response.recipeimport.RecipeImportJobResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service implementation managing smart recipe-import jobs: starting a job (with per-user rate
 * limiting), and the background pipeline that fetches content, extracts structured data via
 * {@link RecipeExtractionProvider}, resolves ingredient units against the real {@code units}
 * table, and creates the result as a private recipe through the existing
 * {@link RecipeService#createRecipe} — the exact same path a manually-created recipe takes, which
 * is precisely why an imported recipe composes with translation (Phase 2) and portion scaling
 * (Phase 3) with no additional code.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeImportServiceImp implements RecipeImportService {

    /** Per-user cap on LLM/OCR-cost-bearing import jobs within {@link #RATE_LIMIT_WINDOW_HOURS}. */
    private static final int MAX_IMPORTS_PER_WINDOW = 5;
    private static final int RATE_LIMIT_WINDOW_HOURS = 1;
    /** Used whenever an extracted ingredient's unit doesn't match a real unit code. */
    private static final String FALLBACK_UNIT_CODE = "piece";
    /** Hard cap on photos in a single PHOTO import, bounding Gemini payload size and cost. */
    private static final int MAX_PHOTOS_PER_IMPORT = 5;

    // Stable, machine-readable failure codes stored as the job's errorCode — English-only and
    // never shown verbatim, exactly like ApiErrorResponse.errorCode elsewhere in this app; the
    // client resolves each one to a localized string (see RecipeImportViewModel).
    private static final String ERROR_ROBOTS_DISALLOWED = "ROBOTS_DISALLOWED";
    private static final String ERROR_NO_RECIPE_FOUND_PAGE = "NO_RECIPE_FOUND_PAGE";
    private static final String ERROR_NO_RECIPE_FOUND_PHOTO = "NO_RECIPE_FOUND_PHOTO";
    private static final String ERROR_FETCH_FAILED_PAGE = "FETCH_FAILED_PAGE";
    private static final String ERROR_FETCH_FAILED_PHOTO = "FETCH_FAILED_PHOTO";
    private static final String ERROR_IMPORT_FAILED_GENERIC = "IMPORT_FAILED_GENERIC";
    /** The extraction call itself failed (network, quota, transient outage) — distinct from
     *  "the content genuinely isn't a recipe" so the user gets an honest reason. */
    private static final String ERROR_EXTRACTION_SERVICE_UNAVAILABLE = "EXTRACTION_SERVICE_UNAVAILABLE";

    private final RecipeImportJobRepository recipeImportJobRepository;
    private final UserRepository userRepository;
    private final UnitRepository unitRepository;
    private final RecipeService recipeService;
    private final RecipeExtractionProvider extractionProvider;
    private final WebPageTextExtractor webPageTextExtractor;
    private final RobotsTxtChecker robotsTxtChecker;
    private final RestClient imageDownloadClient = RestClient.create();

    /**
     * The bounded broadcast executor from {@code AsyncConfig} — currently the only
     * {@link Executor}-typed bean in the context (see {@code PushNotificationServiceImp} for the
     * same pattern). Submitted to directly via {@link Executor#execute} rather than {@code
     * @Async}, since an {@code @Async} method called from within this same class (as
     * {@link #startImport} calling {@link #processImport} would be) bypasses Spring's proxy and
     * would silently run synchronously instead of in the background.
     */
    private final Executor notificationExecutor;

    @Override
    @Transactional
    public RecipeImportJobResponse startImport(RecipeImportStartRequestDTO request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(EntityNames.USER, userEmail));

        RecipeImportJob.SourceType sourceType = RecipeImportJob.SourceType.valueOf(request.sourceType());
        if (sourceType == RecipeImportJob.SourceType.WEB && !StringUtils.hasText(request.sourceUrl())) {
            throw new IllegalArgumentException("sourceUrl is required for a WEB import");
        }
        List<String> sourceImageUrls = request.sourceImageUrls() == null ? List.of() : request.sourceImageUrls();
        if (sourceType == RecipeImportJob.SourceType.PHOTO && sourceImageUrls.isEmpty()) {
            throw new IllegalArgumentException("sourceImageUrls is required for a PHOTO import");
        }
        if (sourceImageUrls.size() > MAX_PHOTOS_PER_IMPORT) {
            throw new IllegalArgumentException("A single import can use at most " + MAX_PHOTOS_PER_IMPORT + " photos");
        }

        long recentImports = recipeImportJobRepository.countByUserIdAndCreatedAtAfter(
                user.getId(), LocalDateTime.now().minusHours(RATE_LIMIT_WINDOW_HOURS));
        if (recentImports >= MAX_IMPORTS_PER_WINDOW) {
            throw new RateLimitExceededException(
                    "You've reached the recipe-import limit (" + MAX_IMPORTS_PER_WINDOW + " per hour). Please try again later.");
        }

        RecipeImportJob job = RecipeImportJob.builder()
                .user(user)
                .sourceType(sourceType)
                .sourceUrl(request.sourceUrl())
                .status(RecipeImportJob.Status.PENDING)
                .build();
        for (int i = 0; i < sourceImageUrls.size(); i++) {
            job.getSourceImages().add(RecipeImportJobImage.builder()
                    .job(job)
                    .imageUrl(sourceImageUrls.get(i))
                    .sortOrder(i)
                    .build());
        }
        RecipeImportJob saved = recipeImportJobRepository.save(job);

        String jobId = saved.getId();
        dispatchProcessing(jobId);

        return RecipeImportJobMapper.toResponse(saved);
    }

    /**
     * Dispatches {@link #processImport} to {@link #notificationExecutor}, deferring the dispatch
     * until this method's surrounding {@code @Transactional} transaction actually commits.
     * Without this, the background thread's own {@code findWithUserById} call — running in its
     * own transaction on a separate connection — can race the still-open {@link #startImport}
     * transaction and see the just-inserted job row as not-yet-committed, silently leaving the
     * job stuck in {@code PENDING} forever (observed live: a background thread logging the job
     * as "vanished" within milliseconds of it being created). {@link #startImport}'s unit tests
     * call this service directly rather than through Spring's transactional proxy, so no
     * synchronization is active there — {@code isSynchronizationActive()} is {@code false} and
     * this falls back to dispatching immediately, matching those tests' existing expectations.
     *
     * @param jobId the job to process once the current transaction (if any) commits
     */
    private void dispatchProcessing(String jobId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notificationExecutor.execute(() -> processImport(jobId));
                }
            });
        } else {
            notificationExecutor.execute(() -> processImport(jobId));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeImportJobResponse getJobStatus(String jobId, String userEmail) {
        RecipeImportJob job = OwnershipValidator.requireOwnedResource(
                () -> recipeImportJobRepository.findById(jobId), EntityNames.RECIPE_IMPORT_JOB, jobId,
                j -> j.getUser().getId(), userRepository, userEmail,
                "You are not allowed to view this import job.");
        return RecipeImportJobMapper.toResponse(job);
    }

    /**
     * The background pipeline: robots.txt check (WEB only) → fetch content → extract → resolve
     * units → create the recipe (forced PRIVATE) → attribute it → mark the job SUCCEEDED. Any
     * failure along the way marks the job FAILED with a stable error code instead of leaving it
     * stuck in PROCESSING.
     *
     * @param jobId the job to process
     */
    void processImport(String jobId) {
        RecipeImportJob job = recipeImportJobRepository.findWithUserById(jobId).orElse(null);
        if (job == null) {
            log.warn("Recipe import job {} vanished before processing could start", jobId);
            return;
        }
        job.setStatus(RecipeImportJob.Status.PROCESSING);
        recipeImportJobRepository.save(job);

        try {
            List<String> unitCodes = unitRepository.findAll().stream().map(Unit::getCode).toList();
            String fallbackImageUrl = null;
            ExtractedRecipe extracted;

            if (job.getSourceType() == RecipeImportJob.SourceType.WEB) {
                if (!robotsTxtChecker.isAllowed(job.getSourceUrl())) {
                    failJob(job, ERROR_ROBOTS_DISALLOWED);
                    return;
                }
                WebPageTextExtractor.PageContent content = webPageTextExtractor.fetchReadableContent(job.getSourceUrl());
                fallbackImageUrl = content.imageUrl();
                extracted = extractionProvider.extractFromWebPage(content.text(), unitCodes).orElse(null);
            } else {
                List<ImageInput> images = new ArrayList<>();
                for (RecipeImportJobImage sourceImage : job.getSourceImages()) {
                    byte[] imageBytes = imageDownloadClient.get().uri(sourceImage.getImageUrl())
                            .retrieve().body(byte[].class);
                    images.add(new ImageInput(imageBytes, guessMimeType(sourceImage.getImageUrl())));
                }
                fallbackImageUrl = job.getSourceImages().get(0).getImageUrl();
                extracted = extractionProvider.extractFromImages(images, unitCodes).orElse(null);
            }

            if (extracted == null) {
                failJob(job, job.getSourceType() == RecipeImportJob.SourceType.WEB
                        ? ERROR_NO_RECIPE_FOUND_PAGE : ERROR_NO_RECIPE_FOUND_PHOTO);
                return;
            }

            RecipeCreateRequestDTO createRequest = toCreateRequest(extracted, fallbackImageUrl);
            RecipeResponse created = recipeService.createRecipe(createRequest, job.getUser().getEmail());

            String attributionUrl = job.getSourceType() == RecipeImportJob.SourceType.WEB
                    ? job.getSourceUrl() : job.getSourceImages().get(0).getImageUrl();
            String attributionNote = job.getSourceType() == RecipeImportJob.SourceType.WEB
                    ? "Imported from " + hostOf(job.getSourceUrl())
                    : job.getSourceImages().size() == 1 ? "Imported from a photo" : "Imported from photos";
            recipeService.setSourceAttribution(created.id(), attributionUrl, attributionNote);

            job.setStatus(RecipeImportJob.Status.SUCCEEDED);
            job.setResultRecipeId(created.id());
            recipeImportJobRepository.save(job);
        } catch (IOException e) {
            log.warn("Recipe import job {} couldn't fetch its source: {}", jobId, e.getMessage());
            failJob(job, job.getSourceType() == RecipeImportJob.SourceType.WEB
                    ? ERROR_FETCH_FAILED_PAGE : ERROR_FETCH_FAILED_PHOTO);
        } catch (ExtractionServiceException e) {
            log.warn("Recipe import job {} hit an extraction-service problem: {}", jobId, e.getMessage());
            failJob(job, ERROR_EXTRACTION_SERVICE_UNAVAILABLE);
        } catch (RuntimeException e) {
            log.warn("Recipe import job {} failed: {}", jobId, e.getMessage());
            failJob(job, ERROR_IMPORT_FAILED_GENERIC);
        }
    }

    /**
     * Converts extraction output into the same request shape a manually-created recipe uses,
     * resolving each ingredient's unit code against the real {@code units} table and forcing
     * {@code PRIVATE} visibility regardless of what (if anything) the extraction produced —
     * every imported recipe must pass through human review before it can ever be published.
     *
     * @param extracted the extraction provider's output
     * @param fallbackImageUrl a cover image to use if the extraction has none of its own (the
     *                         source page's og:image, or the user's own uploaded photo)
     * @return the request, ready for {@link RecipeService#createRecipe}
     */
    private RecipeCreateRequestDTO toCreateRequest(ExtractedRecipe extracted, String fallbackImageUrl) {
        List<IngredientRequestDTO> ingredients = new ArrayList<>();
        for (ExtractedRecipe.ExtractedIngredient ingredient : extracted.ingredients()) {
            String unitId = unitRepository.findByCodeIgnoreCase(ingredient.unitCode())
                    .or(() -> unitRepository.findByCodeIgnoreCase(FALLBACK_UNIT_CODE))
                    .map(Unit::getId)
                    .orElse(null);
            ingredients.add(new IngredientRequestDTO(
                    UUID.randomUUID().toString(), ingredient.name(),
                    ingredient.quantity() == null ? 1.0 : ingredient.quantity().doubleValue(), unitId));
        }

        List<InstructionRequestDTO> instructions = new ArrayList<>();
        int stepNumber = 1;
        for (String step : extracted.instructions()) {
            instructions.add(new InstructionRequestDTO(stepNumber++, step, false, null, List.of(), null));
        }
        if (instructions.isEmpty()) {
            instructions.add(new InstructionRequestDTO(1, "No instructions were found — please add them.", false, null, List.of(), null));
        }

        List<DescriptionBlockDTO> descriptionBlocks = StringUtils.hasText(extracted.description())
                ? List.of(new DescriptionBlockDTO("TEXT", extracted.description(), null, null, false))
                : List.of();

        return new RecipeCreateRequestDTO(
                extracted.title(),
                extracted.difficulty(),
                "PRIVATE",
                extracted.prepTimeMinutes(),
                extracted.cookTimeMinutes(),
                extracted.servings(),
                List.of(),
                ingredients,
                instructions,
                fallbackImageUrl,
                descriptionBlocks);
    }

    private void failJob(RecipeImportJob job, String errorCode) {
        job.setStatus(RecipeImportJob.Status.FAILED);
        job.setErrorMessage(errorCode);
        recipeImportJobRepository.save(job);
    }

    private static String hostOf(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? url : host;
        } catch (RuntimeException e) {
            return url;
        }
    }

    private static String guessMimeType(String imageUrl) {
        String lower = imageUrl.toLowerCase(java.util.Locale.ROOT);
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".heic") || lower.endsWith(".heif")) {
            return "image/heic";
        }
        return "image/jpeg";
    }
}
