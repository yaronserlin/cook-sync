package com.cooksync.app.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.cooksync.app.data.repository.AnnouncementRepository;
import com.cooksync.app.data.repository.BaseRepository;
import com.cooksync.app.data.repository.DeviceTokenRepository;
import com.cooksync.app.data.repository.RecipeRepository;
import com.cooksync.app.data.repository.TagRepository;
import com.cooksync.app.data.service.RecipePublishManager;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.domain.Event;
import com.cooksync.app.domain.FeedState;
import com.cooksync.app.ui.base.AbstractFilterableListViewModel;
import com.cooksync.app.util.PendingActionScheduler;
import com.cooksync.app.util.RecipeFilterUtils;
import com.cooksync.app.util.constants.PaginationConstants;
import com.dtos.response.PagedResponse;
import com.dtos.response.announcement.AnnouncementResponse;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.dtos.response.recipe.RecipeResponse;
import com.dtos.response.tags.TagResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-layer (Android) ViewModel backing {@link HomeActivity}: owns the Home/Discover feed's
 * data state, including paginated recipe-feed loading against the server's browse and
 * tag-filtered endpoints (via {@link RecipeRepository}, returning {@link RecipePreviewResponse}
 * DTOs shared with the server), the available-tags catalog (via {@link TagRepository}), the
 * favorites set with an optimistic add / deferred-remove-with-undo flow, and the
 * sort/difficulty/rating/time/tag filtering inherited from {@link AbstractFilterableListViewModel}.
 * Keyword search lives on the dedicated search screen instead.
 *
 * @author Yaron Serlin
 * @version 1.2
 * @since 04/08/2026
 */
public class HomeViewModel extends AbstractFilterableListViewModel {

    private final PendingActionScheduler pendingActions = new PendingActionScheduler();
    private final RecipeRepository recipeRepository;
    private final TagRepository tagRepository;
    private final AnnouncementRepository announcementRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    private final MutableLiveData<FeedState> feedState = new MutableLiveData<>(new FeedState.Idle());
    private final MutableLiveData<ApiResult<List<TagResponse>>> tagsResult = new MutableLiveData<>();
    private final MutableLiveData<ApiResult<List<RecipePreviewResponse>>> favoritesResult = new MutableLiveData<>();
    private final MutableLiveData<Event<String>> errorEvent = new MutableLiveData<>();
    private final MutableLiveData<Event<AnnouncementResponse>> announcementEvent = new MutableLiveData<>();

    private final List<RecipePreviewResponse> currentRecipes = new ArrayList<>();
    private int currentPage = 0;
    private boolean isLastPage = false;

    /**
     * Kept as a field so it can be detached in {@link #onCleared()} — {@link RecipePublishManager}
     * is a process-wide singleton, so an observer registered via {@code observeForever} and never
     * removed would keep this ViewModel (and everything it references) alive indefinitely.
     */
    private final Observer<Event<RecipeResponse>> recipePublishedObserver = event -> {
        if (event != null && event.getContentIfNotHandled() != null) {
            loadInitialFeed();
        }
    };

    /**
     * Constructs the ViewModel with the given repositories, injected by
     * {@link com.cooksync.app.ui.base.ViewModelFactory}.
     *
     * @param recipeRepository the repository used for feed/favorite calls
     * @param tagRepository the repository used to load the available tags
     * @param announcementRepository the repository used to check for a pending system announcement
     * @param deviceTokenRepository the repository used to register this device's push token
     */
    public HomeViewModel(RecipeRepository recipeRepository, TagRepository tagRepository,
                          AnnouncementRepository announcementRepository,
                          DeviceTokenRepository deviceTokenRepository) {
        this.recipeRepository = recipeRepository;
        this.tagRepository = tagRepository;
        this.announcementRepository = announcementRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        RecipePublishManager.getInstance().getRecipePublishedEvent().observeForever(recipePublishedObserver);
    }

    /** @return the current feed loading/success/error state */
    public LiveData<FeedState> getFeedState() { return feedState; }

    /** @return the tag catalog load result, used to populate the tag chip row */
    public LiveData<ApiResult<List<TagResponse>>> getTagsResult() { return tagsResult; }

    /** @return the current favorites set, kept in sync with {@link #toggleFavorite} */
    public LiveData<ApiResult<List<RecipePreviewResponse>>> getFavoritesResult() { return favoritesResult; }

    /**
     * One-off error notifications (e.g. a failed favorite toggle) meant to be shown once
     * (a Toast/Snackbar) rather than re-delivered on every observer re-attachment.
     *
     * @return the error event stream
     */
    public LiveData<Event<String>> getErrorEvent() { return errorEvent; }

    /**
     * One-off notification that a system announcement is pending and should be shown as a
     * dialog — fires at most once per {@link #checkActiveAnnouncement()} call, never re-delivered
     * on rotation/re-attachment.
     *
     * @return the pending-announcement event stream
     */
    public LiveData<Event<AnnouncementResponse>> getAnnouncementEvent() { return announcementEvent; }

    /**
     * Resets pagination, then reloads the feed from the first page. The active
     * sort/difficulty/tag selections are left untouched.
     */
    public void loadInitialFeed() { refresh(); }

    /**
     * Fetches the next page of recipes if there are more available and no request
     * is currently in flight. A no-op whenever the current view (search results, or a
     * single-tag filter) was fetched in full rather than paginated.
     */
    public void loadNextPage() {
        if (isLastPage || feedState.getValue() instanceof FeedState.Loading) {
            return;
        }
        currentPage++;
        fetchNextPage();
    }

    /**
     * Toggles a single tag's membership in {@link #selectedTags} (multi-select) and refreshes
     * the feed. Unlike the old single-tag model, this never clears sort/difficulty or the
     * other selected tags.
     *
     * @param tagName the tag to toggle on/off
     */
    public void toggleTag(String tagName) {
        if (tagName == null) return;
        if (!selectedTags.remove(tagName)) {
            selectedTags.add(tagName);
        }
        refresh();
    }

    /**
     * Clears every selected tag (the Home tag row's "All" chip) and refreshes the feed.
     */
    public void clearTags() {
        if (selectedTags.isEmpty()) return;
        selectedTags.clear();
        refresh();
    }

    /** Fetches the full tag catalog for {@link #getTagsResult()}, used to populate the tag chip row. */
    public void loadTags() {
        tagRepository.getAllTags(tagsResult);
    }

    /**
     * Fetches the current favorites set for {@link #getFavoritesResult()}, used by
     * {@link HomeActivity} to render each feed card's heart icon filled or outlined.
     */
    public void loadFavorites() {
        recipeRepository.getFavorites(favoritesResult);
    }

    /**
     * Checks for a pending system announcement (one the user hasn't dismissed yet) and, if one
     * exists, fires {@link #announcementEvent} so the view can show it as a dialog. Called once
     * per {@link HomeActivity#onResume()} — a rare fallback for anyone who missed the push
     * notification itself, so a failure here is silently ignored rather than surfaced as an
     * error.
     */
    public void checkActiveAnnouncement() {
        MutableLiveData<ApiResult<AnnouncementResponse>> result = new MutableLiveData<>();
        observeOnce(result, apiResult -> {
            if (apiResult instanceof ApiResult.Success<AnnouncementResponse> success && success.getData() != null) {
                announcementEvent.setValue(new Event<>(success.getData()));
            }
        });
        announcementRepository.getActiveAnnouncement(result);
    }

    /**
     * Records that the user dismissed ("Got it") the given announcement, so it isn't shown
     * again.
     *
     * @param announcementId the announcement's ID
     */
    public void dismissAnnouncement(String announcementId) {
        announcementRepository.dismiss(announcementId, new MutableLiveData<>());
    }

    /**
     * Registers this device's current FCM push token against the authenticated user's account.
     * Called once per app start; failures are silently ignored (retried naturally on the next
     * app start) rather than surfaced, since a missed registration only delays push delivery to
     * this device, not a user-facing action.
     *
     * @param pushToken the device's current FCM registration token
     */
    public void registerDeviceToken(String pushToken) {
        deviceTokenRepository.registerDevice(pushToken, "ANDROID", new MutableLiveData<>());
    }

    /**
     * Re-fetches the feed from the first page so a filter-sheet change or per-dimension removal
     * takes effect immediately. The server's browse/search/tag endpoints don't accept sort,
     * difficulty, or multi-tag parameters, so filtering and sorting happen client-side via
     * {@link RecipeFilterUtils} in {@link #applyFiltersAndSort} — server-side support for these
     * would let filtering cover the full catalog immediately instead of only the pages loaded so
     * far via scrolling.
     */
    @Override
    protected void onFiltersChanged() {
        refresh();
    }

    /**
     * Resets pagination and re-fetches the first page from whichever endpoint matches the
     * current mode: a single selected tag hits the tag-filtered endpoint, otherwise the general
     * browse feed. Multi-tag selections (2+) fall back to the paginated feed with client-side
     * filtering, since there is no multi-tag server endpoint.
     */
    private void refresh() {
        currentPage = 0;
        isLastPage = false;
        currentRecipes.clear();
        fetchNextPage();
    }

    /**
     * Fetches the page at {@link #currentPage} from the endpoint matching the current
     * selection (single-tag filter vs. general feed) and merges it into {@link #currentRecipes}.
     * Both endpoints return the same {@link PagedResponse} shape, so a single accumulation/
     * pagination path serves either source.
     */
    private void fetchNextPage() {
        feedState.setValue(new FeedState.Loading(currentPage == 0));
        MutableLiveData<ApiResult<PagedResponse<RecipePreviewResponse>>> result = new MutableLiveData<>();

        observeOnce(result, apiResult -> {
            if (apiResult instanceof ApiResult.Success<PagedResponse<RecipePreviewResponse>> success) {
                PagedResponse<RecipePreviewResponse> page = success.getData();
                currentRecipes.addAll(page.content());
                isLastPage = page.last();
                feedState.postValue(new FeedState.Success(applyFiltersAndSort(currentRecipes), !isLastPage));
            } else if (apiResult instanceof ApiResult.Error<PagedResponse<RecipePreviewResponse>> error) {
                feedState.postValue(new FeedState.Error(error.getMessage()));
            }
        });

        if (selectedTags.size() == 1) {
            recipeRepository.getRecipesByTag(selectedTags.iterator().next(), currentPage, PaginationConstants.PAGE_SIZE, result);
        } else {
            recipeRepository.getPublicFeed(currentPage, PaginationConstants.PAGE_SIZE, result);
        }
    }

    /**
     * Filters {@code source} by the active difficulty/tag selection and sorts it per the
     * active sort choice, via {@link RecipeFilterUtils#applyFiltersAndSort}. {@code source}
     * itself (the raw accumulated page cache) is left untouched so further pagination keeps
     * working against the full set.
     *
     * @param source the raw, unfiltered recipes accumulated so far
     * @return a filtered, sorted copy ready to display
     */
    private List<RecipePreviewResponse> applyFiltersAndSort(List<RecipePreviewResponse> source) {
        return RecipeFilterUtils.applyFiltersAndSort(source, currentDifficulty, currentMinRating,
                currentMaxTotalTimeMinutes, selectedTags, currentSort);
    }

    /**
     * Optimistically toggles a recipe's favorite state in {@link #favoritesResult}. Adding a
     * favorite is sent immediately; removing one is deferred by {@link BaseRepository#UNDO_WINDOW_MS}
     * instead, so a tap on the toast's "Undo" action (see {@link #undoRemoveFavorite}) can
     * cancel it before it's ever sent. If an add is requested while its matching remove is
     * still pending, the pending remove is simply cancelled rather than sending an add for
     * something the server still has. If a server call that does go out fails, the optimistic
     * change is rolled back and {@link #errorEvent} is emitted so the UI can inform the user.
     *
     * @param recipeId the id of the recipe to favorite/unfavorite
     */
    public void toggleFavorite(String recipeId) {
        List<RecipePreviewResponse> previous =
                favoritesResult.getValue() instanceof ApiResult.Success<List<RecipePreviewResponse>> success
                        ? new ArrayList<>(success.getData())
                        : new ArrayList<>();

        boolean isFavorite = previous.stream().anyMatch(r -> r.id().equals(recipeId));

        if (isFavorite) {
            List<RecipePreviewResponse> withoutRecipe = new ArrayList<>(previous);
            withoutRecipe.removeIf(r -> r.id().equals(recipeId));
            favoritesResult.setValue(new ApiResult.Success<>(withoutRecipe));

            pendingActions.schedule(recipeId, BaseRepository.UNDO_WINDOW_MS, () -> {
                MutableLiveData<ApiResult<Void>> writeResult = new MutableLiveData<>();
                observeOnce(writeResult, result -> {
                    if (result instanceof ApiResult.Error<Void> error) {
                        favoritesResult.setValue(new ApiResult.Success<>(previous));
                        errorEvent.setValue(new Event<>(error.getMessage()));
                    }
                });
                recipeRepository.removeFavorite(recipeId, writeResult);
            });
        } else {
            List<RecipePreviewResponse> withRecipe = new ArrayList<>(previous);
            currentRecipes.stream()
                    .filter(r -> r.id().equals(recipeId))
                    .findFirst()
                    .ifPresent(withRecipe::add);
            favoritesResult.setValue(new ApiResult.Success<>(withRecipe));

            if (pendingActions.cancel(recipeId)) {
                return;
            }

            MutableLiveData<ApiResult<Void>> writeResult = new MutableLiveData<>();
            observeOnce(writeResult, result -> {
                if (result instanceof ApiResult.Error<Void> error) {
                    favoritesResult.setValue(new ApiResult.Success<>(previous));
                    errorEvent.setValue(new Event<>(error.getMessage()));
                }
            });
            recipeRepository.addFavorite(recipeId, writeResult);
        }
    }

    /**
     * Cancels a still-pending "remove from favorites" and restores the favorite state. Does
     * nothing if the undo window already elapsed and the remove reached the server.
     *
     * @param recipeId the id of the recipe whose favorite-remove should be undone
     */
    public void undoRemoveFavorite(String recipeId) {
        if (!pendingActions.cancel(recipeId)) return;
        recipeRepository.getFavorites(favoritesResult);
    }

    /**
     * Flushes any still-pending favorite adds immediately rather than dropping them, so
     * navigating away before the undo window elapses doesn't silently discard an add the user
     * never undid.
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        pendingActions.flushAll();
        RecipePublishManager.getInstance().getRecipePublishedEvent().removeObserver(recipePublishedObserver);
    }
}
