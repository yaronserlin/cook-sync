package com.cooksync.app.data.datasource.remote;

/**
 * Aggregate Retrofit contract for every REST endpoint the app depends on. Composed entirely of
 * feature-scoped sub-interfaces — {@link AuthApiService}, {@link MediaApiService},
 * {@link RecipeApiService}, {@link TagApiService}, {@link UnitApiService},
 * {@link FavoriteApiService}, {@link NoteApiService}, {@link ReviewApiService},
 * {@link AdminApiService}, {@link DeviceApiService}, {@link AnnouncementApiService},
 * {@link NotificationPreferencesApiService}, {@link AppConfigApiService}, and
 * {@link RecipeImportApiService} — so each area's endpoints and Javadoc live in a focused file.
 * {@link RetrofitClient} still builds a single dynamic proxy implementing all of them via
 * {@code Retrofit.create(ApiService.class)}, so every existing caller keeps working unchanged.
 *
 * @author Yaron Serlin
 * @version 2.0
 * @since 04/08/2026
 */
public interface ApiService extends
        AuthApiService,
        MediaApiService,
        RecipeApiService,
        TagApiService,
        UnitApiService,
        FavoriteApiService,
        NoteApiService,
        ReviewApiService,
        AdminApiService,
        DeviceApiService,
        AnnouncementApiService,
        NotificationPreferencesApiService,
        AppConfigApiService,
        RecipeImportApiService {
}
