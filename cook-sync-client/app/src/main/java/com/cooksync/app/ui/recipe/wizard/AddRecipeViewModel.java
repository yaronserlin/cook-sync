package com.cooksync.app.ui.recipe.wizard;
import com.cooksync.app.ui.base.BaseViewModel;
import com.cooksync.app.ui.base.ViewModelFactory;
import com.cooksync.app.data.model.recipe.RecipeDraft;
import com.cooksync.app.data.model.recipe.RecipeDraftMediaHelper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.cooksync.app.data.datasource.local.RecipeDraftStore;
import com.cooksync.app.data.repository.RecipeRepository;
import com.cooksync.app.data.repository.TagRepository;
import com.cooksync.app.data.repository.UnitRepository;
import com.cooksync.app.domain.ApiResult;
import com.dtos.response.recipe.DescriptionBlockDTO;
import com.dtos.response.recipe.RecipeResponse;
import com.dtos.response.tags.TagResponse;
import com.dtos.response.unit.UnitResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Activity-scoped ViewModel shared by every step fragment of {@link AddRecipeWizardActivity}.
 * Owns the single {@link RecipeDraft} being authored, exposes it to each fragment for reading
 * and in-place mutation of its own slice, and orchestrates every network call the wizard needs
 * (tags, units, Cloudinary signatures, publishing) plus local draft persistence via
 * {@link RecipeDraftStore}.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 08/08/2026
 */
public class AddRecipeViewModel extends BaseViewModel {

    private final RecipeRepository recipeRepository;
    private final TagRepository tagRepository;
    private final UnitRepository unitRepository;

    private RecipeDraft draft = new RecipeDraft();

    private final MutableLiveData<ApiResult<List<TagResponse>>> tagsResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<List<TagResponse>>> popularTagsResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<List<UnitResponse>>> unitsResult = new MutableLiveData<>();

    /**
     * Constructs the ViewModel with the given repositories, injected by
     * {@link com.cooksync.app.ui.base.ViewModelFactory}.
     *
     * @param recipeRepository the repository used to publish the finished recipe
     * @param tagRepository the repository used to load/create tags
     * @param unitRepository the repository used to load measurement units
     */
    public AddRecipeViewModel(RecipeRepository recipeRepository, TagRepository tagRepository,
                               UnitRepository unitRepository) {
        this.recipeRepository = recipeRepository;
        this.tagRepository = tagRepository;
        this.unitRepository = unitRepository;
    }


    /**
     * Returns whether at least one locally saved draft exists to resume, without loading any of
     * them.
     *
     * @return {@code true} if any resumable draft is stored on-device
     */
    public boolean hasResumableDraft() {
        return RecipeDraftStore.hasDraft();
    }

    /**
     * Starts a brand-new, empty draft. Called when {@link AddRecipeWizardActivity} launches for
     * "Create recipe" (as opposed to resuming a specific saved draft or editing an existing
     * recipe), so an arbitrary number of drafts can coexist without one silently reusing another.
     */
    public void startNewDraft() {
        draft = new RecipeDraft();
    }

    /**
     * Loads one specific stored draft by id into this ViewModel, or starts a fresh draft if it
     * can no longer be found (e.g. already published/discarded elsewhere).
     *
     * @param draftId the draft's client-generated id, as passed to
     *                 {@link AddRecipeWizardActivity#startResumeDraft}
     */
    public void loadDraft(String draftId) {
        RecipeDraft stored = RecipeDraftStore.load(draftId);
        draft = stored != null ? stored : new RecipeDraft();
    }

    /**
     * Seeds the ViewModel with an existing recipe to edit.
     *
     * @param recipe the recipe to edit
     */
    public void startEditRecipe(RecipeResponse recipe) {
        this.draft = RecipeDraft.fromRecipeResponse(recipe);
    }

    /** @return the draft currently being authored */
    public RecipeDraft getDraft() {
        return draft;
    }

    /**
     * Records which step the user last reached, so resuming a saved draft reopens there.
     *
     * @param step the current wizard step index (0-3)
     */
    public void setLastReachedStep(int step) {
        draft.lastReachedStep = step;
    }

    /** Persists the current draft locally. Purely local — no network call, instant. */
    public void saveDraftLocally() {
        draft.savedAtMillis = System.currentTimeMillis();
        RecipeDraftStore.save(draft);
    }

    /** Discards the current draft, both in-memory and on-device. Other saved drafts are unaffected. */
    public void discardDraft() {
        RecipeDraftStore.remove(draft.draftId);
        RecipeImagePicker.clearCache(com.cooksync.app.CookSyncApplication.getAppContext());
        draft = new RecipeDraft();
    }


    /**
     * Sets the recipe's title.
     *
     * @param title the new title text
     */
    public void setTitle(String title) {
        draft.title = title;
    }

    /**
     * @return the draft's description blocks (TEXT and IMAGE, in author-defined order) — the
     *         live list, so a {@link WizardDescriptionBlockAdapter} can reorder it directly
     */
    public List<DescriptionBlockDTO> getDescriptionBlocks() {
        return draft.descriptionBlocks;
    }

    /**
     * Updates a TEXT block's content. {@link DescriptionBlockDTO} is an immutable record, so
     * this replaces the block in place with a copy carrying the new text.
     *
     * @param block the TEXT block being edited
     * @param text the block's new text content
     */
    public void updateDescriptionBlockText(DescriptionBlockDTO block, String text) {
        replaceDescriptionBlock(block, new DescriptionBlockDTO(block.type(), text, block.imageUrl(), block.caption(), false));
    }

    /**
     * Updates an IMAGE block's caption. {@link DescriptionBlockDTO} is an immutable record, so
     * this replaces the block in place with a copy carrying the new caption.
     *
     * @param block the IMAGE block being edited
     * @param caption the block's new caption text
     */
    public void setDescriptionImageCaption(DescriptionBlockDTO block, String caption) {
        replaceDescriptionBlock(block, new DescriptionBlockDTO(block.type(), block.text(), block.imageUrl(), caption, false));
    }

    /**
     * Replaces {@code oldBlock} with {@code newBlock} at the same position (matched by
     * {@code equals} — in practice unique per block since each TEXT/IMAGE block carries its own
     * content/URL).
     *
     * @param oldBlock the block to replace
     * @param newBlock the block to put in its place
     */
    private void replaceDescriptionBlock(DescriptionBlockDTO oldBlock, DescriptionBlockDTO newBlock) {
        int index = draft.descriptionBlocks.indexOf(oldBlock);
        if (index >= 0) {
            draft.descriptionBlocks.set(index, newBlock);
        }
    }

    /**
     * Appends an inline description photo, referenced for now by its local picked-file URI —
     * nothing is uploaded to Cloudinary until Publish (see
     * {@link com.cooksync.app.data.service.RecipePublishManager}), matching the wizard's "nothing
     * leaves the device before Publish" rule. Matching the design's own copy ("Photos sit inline
     * where you place them. The first one becomes the cover if you don't set one."), this becomes
     * the cover photo too if none was explicitly chosen yet.
     *
     * @param localUri the picked image's local (file://) URI, as a string
     */
    public void addDescriptionImage(String localUri) {
        draft.descriptionBlocks.add(new DescriptionBlockDTO("IMAGE", null, localUri, null, false));
        if (draft.primaryImageUrl == null || draft.primaryImageUrl.isEmpty()) {
            draft.primaryImageUrl = localUri;
        }
    }

    /**
     * Removes an IMAGE description block.
     *
     * @param block the IMAGE block to remove
     */
    public void removeDescriptionImage(DescriptionBlockDTO block) {
        draft.descriptionBlocks.remove(block);
    }

    /**
     * Removes an already-empty TEXT block the author backspaced out of — the inverse of
     * {@link #splitDescriptionTextBlock}. A single remaining block is never removed, so the
     * description always has somewhere to type.
     *
     * @return the index focus should move to (the previous block, so backspacing merges into it
     *         like a text editor), or -1 if {@code block} wasn't removed
     */
    public int removeDescriptionTextBlock(DescriptionBlockDTO block) {
        if (draft.descriptionBlocks.size() <= 1) return -1;
        int index = draft.descriptionBlocks.indexOf(block);
        if (index < 0) return -1;
        draft.descriptionBlocks.remove(index);
        return Math.max(0, index - 1);
    }

    /**
     * Splits a TEXT block into two at the point the author pressed Enter, so each paragraph is
     * its own block and inline photos can be dragged between them — the same effect as adding a
     * new empty TEXT block via the wizard's "add block" button, but authored inline while typing
     * rather than via a separate button.
     *
     * @param block the TEXT block being edited
     * @param beforeText the text kept in {@code block}, before the cursor
     * @param afterText the text moved into the new block, after the cursor
     * @return the index the new block was inserted at, or -1 if {@code block} was no longer present
     */
    public int splitDescriptionTextBlock(DescriptionBlockDTO block, String beforeText, String afterText) {
        int index = draft.descriptionBlocks.indexOf(block);
        if (index < 0) return -1;
        draft.descriptionBlocks.set(index, new DescriptionBlockDTO(block.type(), beforeText, block.imageUrl(), block.caption(), false));
        draft.descriptionBlocks.add(index + 1, new DescriptionBlockDTO("TEXT", afterText, null, null, false));
        return index + 1;
    }

    /** @return the draft's plain-text description, i.e. every TEXT block joined together */
    public String getDescriptionSummary() {
        StringBuilder builder = new StringBuilder();
        for (DescriptionBlockDTO block : draft.descriptionBlocks) {
            if ("TEXT".equals(block.type()) && block.text() != null && !block.text().isBlank()) {
                if (builder.length() > 0) builder.append(' ');
                builder.append(block.text().trim());
            }
        }
        return builder.toString();
    }

    /**
     * Sets the recipe's preparation time.
     *
     * @param minutes preparation duration in minutes, or {@code null} if not yet entered
     */
    public void setPrepTimeMinutes(Integer minutes) {
        draft.prepTimeMinutes = minutes;
    }

    /**
     * Sets the recipe's active cooking time.
     *
     * @param minutes cooking duration in minutes, or {@code null} if not yet entered
     */
    public void setCookTimeMinutes(Integer minutes) {
        draft.cookTimeMinutes = minutes;
    }

    /**
     * Sets the recipe's recommended serving yield.
     *
     * @param servings the number of servings, or {@code null} if not yet entered
     */
    public void setServings(Integer servings) {
        draft.servings = servings;
    }

    /**
     * Sets the recipe's difficulty level.
     *
     * @param difficulty one of {@code "EASY"}, {@code "MEDIUM"}, {@code "HARD"}
     */
    public void setDifficulty(String difficulty) {
        draft.difficulty = difficulty;
    }

    /**
     * Sets the recipe's cover photo.
     *
     * @param localUri the picked cover photo's local (file://) URI, as a string — not uploaded
     *                 until Publish (see {@link com.cooksync.app.data.service.RecipePublishManager})
     */
    public void setCoverImageUrl(String localUri) {
        draft.primaryImageUrl = localUri;
    }

    /**
     * Adds a tag to the draft's selection, ignoring the call if it's already selected.
     *
     * @param tag the tag to select
     */
    public void addTag(TagResponse tag) {
        if (draft.tags.stream().noneMatch(t -> Objects.equals(t.id(), tag.id()))) {
            draft.tags.add(tag);
        }
    }

    /**
     * Removes a tag from the draft's selection.
     *
     * @param tag the tag to deselect
     */
    public void removeTag(TagResponse tag) {
        draft.tags.removeIf(t -> Objects.equals(t.id(), tag.id()));
    }

    /**
     * Loads the complete tag catalog for the wizard's tag picker/autocomplete.
     */
    public void loadTags() {
        tagRepository.getAllTags(tagsResult);
    }

    /**
     * Loads the most-used tags across all recipes for the "Popular tags" suggestion row.
     *
     * @param limit maximum number of popular tags to request
     */
    public void loadPopularTags(int limit) {
        tagRepository.getPopularTags(limit, popularTagsResult);
    }

    /**
     * Marks a not-yet-existing tag name as selected, purely locally — matching the rest of the
     * wizard's "nothing hits the server before Publish" rule. The tag is only actually created
     * (via {@link TagRepository#createTag}) as part of the Publish flow (see
     * {@link com.cooksync.app.data.service.RecipePublishManager}).
     *
     * @param name the new tag's name
     */
    public void addPendingTag(String name) {
        if (!draft.pendingNewTagNames.contains(name)) {
            draft.pendingNewTagNames.add(name);
        }
    }

    /**
     * Un-marks a not-yet-existing tag name as selected.
     *
     * @param name the pending tag name to drop
     */
    public void removePendingTag(String name) {
        draft.pendingNewTagNames.remove(name);
    }

    /** @return names of tags the user selected that don't exist server-side yet */
    public List<String> getPendingTagNames() {
        return draft.pendingNewTagNames;
    }

    /** @return observable result of the tag-catalog fetch */
    public LiveData<ApiResult<List<TagResponse>>> getTagsResult() { return tagsResult; }

    /** @return observable result of the popular-tags fetch */
    public LiveData<ApiResult<List<TagResponse>>> getPopularTagsResult() { return popularTagsResult; }


    /** @return the draft's ingredient rows — the live list, so an adapter can reorder it directly */
    public List<RecipeDraft.DraftIngredient> getIngredients() {
        return draft.ingredients;
    }

    /**
     * Appends a new, blank ingredient row to the draft.
     *
     * @return the newly added, blank ingredient row
     */
    public RecipeDraft.DraftIngredient addIngredient() {
        RecipeDraft.DraftIngredient ingredient = new RecipeDraft.DraftIngredient();
        draft.ingredients.add(ingredient);
        return ingredient;
    }

    /** Removes an ingredient and un-links it from every instruction step that referenced it. */
    public void removeIngredient(RecipeDraft.DraftIngredient ingredient) {
        draft.ingredients.remove(ingredient);
        for (RecipeDraft.DraftInstruction instruction : draft.instructions) {
            instruction.linkedIngredientTmpIds.remove(ingredient.tmpId);
        }
    }

    /**
     * Loads the complete measurement-unit catalog for each ingredient row's unit picker.
     */
    public void loadUnits() {
        unitRepository.getAllUnits(unitsResult);
    }

    /** @return observable result of the unit-catalog fetch */
    public LiveData<ApiResult<List<UnitResponse>>> getUnitsResult() { return unitsResult; }


    /** @return the draft's instruction steps — the live list, so an adapter can reorder it directly */
    public List<RecipeDraft.DraftInstruction> getInstructions() {
        return draft.instructions;
    }

    /**
     * Appends a new, blank instruction step to the draft.
     *
     * @return the newly added, blank instruction step
     */
    public RecipeDraft.DraftInstruction addInstruction() {
        RecipeDraft.DraftInstruction instruction = new RecipeDraft.DraftInstruction();
        draft.instructions.add(instruction);
        return instruction;
    }

    /**
     * Removes an instruction step from the draft.
     *
     * @param instruction the instruction step to remove
     */
    public void removeInstruction(RecipeDraft.DraftInstruction instruction) {
        draft.instructions.remove(instruction);
    }


    /**
     * One picked-but-not-yet-uploaded image, identifying exactly where its resulting Cloudinary
     * URL needs to be written back once uploaded. Actually performing the upload requires a
     * {@link android.content.Context} (the Cloudinary Android SDK is inherently
     * Context-dependent), so — matching this app's MVVM boundary — {@link AddRecipeWizardActivity}
     * owns the upload loop itself and calls {@link #resolvePendingImageUpload} as each finishes;
     * this ViewModel only tracks *where* each pending image lives in the draft.
     *
     * @return every draft image still referenced by a local (not-yet-uploaded) URI
     */
    public List<RecipeDraftMediaHelper.PendingImageUpload> collectPendingImageUploads() {
        return RecipeDraftMediaHelper.collectPendingImageUploads(draft);
    }

    /**
     * Writes back a pending image's resulting Cloudinary URL once {@link AddRecipeWizardActivity}
     * finishes uploading it.
     *
     * @param pending the pending upload descriptor being resolved
     * @param uploadedUrl the uploaded asset's secure Cloudinary URL
     */
    public void resolvePendingImageUpload(RecipeDraftMediaHelper.PendingImageUpload pending, String uploadedUrl) {
        RecipeDraftMediaHelper.resolvePendingImageUpload(draft, pending, uploadedUrl);
    }


    /** @return the visibility currently selected ("PUBLIC" or "PRIVATE") */
    public String getVisibility() { return draft.visibility; }

    /**
     * Sets the recipe's visibility.
     *
     * @param visibility the new visibility setting ("PUBLIC" or "PRIVATE")
     */
    public void setVisibility(String visibility) {
        draft.visibility = visibility;
    }

    /**
     * Pads {@code realNames} up to {@code limit} entries using {@code fallbackNames}, skipping
     * any fallback name that's a case-insensitive duplicate of one already present — so the
     * "Popular tags" row never looks sparse when the server has fewer ranked tags than the row
     * wants to show (e.g. a fresh catalog), without ever showing the same tag name twice.
     *
     * @param realNames tag names returned by the server, most-used first
     * @param fallbackNames static fallback names to pad with, in preference order
     * @param limit the maximum number of names to return
     * @return {@code realNames} followed by non-duplicate fallback names, capped at {@code limit}
     */
    public List<String> mergePopularTags(List<String> realNames, List<String> fallbackNames, int limit) {
        List<String> namesToShow = new ArrayList<>(realNames);
        if (namesToShow.size() < limit) {
            for (String fallbackName : fallbackNames) {
                if (namesToShow.size() >= limit) break;
                boolean alreadyShown = namesToShow.stream().anyMatch(shown -> shown.equalsIgnoreCase(fallbackName));
                if (!alreadyShown) {
                    namesToShow.add(fallbackName);
                }
            }
        }
        return namesToShow;
    }
}
