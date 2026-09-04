package com.cooksync.app.ui.settings;

import com.cooksync.app.ui.base.BaseActivity;
import com.cooksync.app.ui.base.Navigator;
import com.cooksync.app.ui.base.ViewModelFactory;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.cooksync.app.R;
import com.cooksync.app.data.datasource.local.CookingPreferencesStore;
import com.cooksync.app.domain.ApiResult;
import com.cooksync.app.ui.admin.AdminConsoleActivity;
import com.cooksync.app.ui.common.FullscreenImageActivity;
import com.cooksync.app.ui.common.OrganicConfirmDialog;
import com.cooksync.app.ui.home.HomeActivity;
import com.cooksync.app.ui.recipe.favorites.FavoriteRecipesActivity;
import com.cooksync.app.ui.recipe.myrecipes.MyRecipesActivity;
import com.cooksync.app.util.GlideUtils;
import com.cooksync.app.util.SessionManager;
import com.dtos.response.recipe.RecipePreviewResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.List;
import java.util.Objects;

/**
 * Hub screen for the "Settings" bottom-navigation tab: the avatar header plus navigational rows
 * to Favorites, My recipes, Cooking preferences, Account details, and, for admin accounts, the
 * Admin console, along with the sign-out action. Avatar and name rendering read from the locally
 * cached session ({@link SessionManager}, populated from the server's
 * {@link com.dtos.response.auth.AuthResponse}) so the screen paints instantly rather than waiting
 * on a network round trip, and picks up whatever was last saved on {@link AccountDetailsActivity}
 * the next time this screen is shown.
 *
 * <p>A newly picked avatar photo is uploaded directly from this device to Cloudinary using a
 * short-lived signature obtained through {@link com.cooksync.app.data.repository.MediaRepository};
 * only the resulting secure URL is ever sent to CookSync's own server, so the image bytes never
 * transit the application backend.</p>
 *
 * <p>Editing of name, city, bio, email, password, and privacy preferences, and account deletion,
 * is handled entirely by the dedicated {@link AccountDetailsActivity} screen, reached from this
 * screen's "Account details" row.</p>
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/08/2026
 */
public class SettingsActivity extends BaseActivity {

    /** Intent extra carrying a one-shot success message to display the next time this screen resumes. */
    public static final String EXTRA_PENDING_TOAST = "extra_pending_toast";

    private SettingsViewModel viewModel;

    private ImageView ivAvatar;
    private TextView tvAvatarInitials;
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvFavoritesSub;
    private TextView tvMyRecipesSub;
    private TextView tvCookingSub;

    private BottomNavigationView bottomNav;

    /**
     * Inflates the screen, wires the ViewModel and every view section, and kicks off the
     * Favorites/My recipes count fetches that populate the row subtitles once they resolve.
     *
     * @param savedInstanceState saved instance state bundle (may be {@code null})
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        viewModel = new ViewModelProvider(this, new ViewModelFactory()).get(SettingsViewModel.class);

        bindViews();
        renderCachedProfile();
        setupBottomNav();
        setupRows();
        setupObservers();

        findViewById(R.id.btn_logout).setOnClickListener(v -> confirmLogout());
        findViewById(R.id.fab_add_recipe).setOnClickListener(v ->
                Navigator.start(this, com.cooksync.app.ui.recipe.wizard.AddRecipeWizardActivity.class));

        viewModel.loadFavoritesCount();
        viewModel.loadMyRecipesCount();
    }

    /**
     * Re-renders the cached profile header, the "Cooking preferences" subtitle, and the
     * Favorites/My recipes counts every time this screen becomes visible again — covering both a
     * fresh navigation back from another tab and a save made on {@link AccountDetailsActivity} or
     * {@link CookingPreferencesActivity} — and shows any pending one-shot toast queued for this
     * resume.
     */
    @Override
    protected void onResume() {
        super.onResume();
        renderCachedProfile();
        refreshCookingPreferencesSub();
        viewModel.loadFavoritesCount();
        viewModel.loadMyRecipesCount();
        showPendingToastIfAny();
    }

    /**
     * Displays and immediately consumes the one-shot message passed via
     * {@link #EXTRA_PENDING_TOAST}, typically set by {@link AccountDetailsActivity} after a
     * successful save. {@link com.cooksync.app.ui.common.OrganicToast} is anchored to the
     * activity that shows it and cannot outlive it, so this is how a save on one screen surfaces
     * its confirmation on the screen the user actually lands on afterward. The extra is stripped
     * from the intent immediately so a later {@code onResume} (rotation, returning from another
     * tab) does not re-show the same message.
     */
    private void showPendingToastIfAny() {
        String message = getIntent().getStringExtra(EXTRA_PENDING_TOAST);
        if (message != null) {
            getIntent().removeExtra(EXTRA_PENDING_TOAST);
            showSuccess(message, bottomNav);
        }
    }

    /**
     * Binds all view references used by the avatar header from the inflated layout.
     */
    private void bindViews() {
        ivAvatar = findViewById(R.id.iv_avatar);
        tvAvatarInitials = findViewById(R.id.tv_avatar_initials);
        tvName = findViewById(R.id.tv_name);
        tvEmail = findViewById(R.id.tv_email);
    }

    /**
     * Paints the avatar header (name, email, avatar image) from whatever is currently cached in
     * {@link SessionManager}, so the screen never has to wait on a network round trip to show the
     * signed-in user's identity.
     */
    private void renderCachedProfile() {
        String first = Objects.requireNonNullElse(SessionManager.getInstance().getFirstName(), "");
        String last = Objects.requireNonNullElse(SessionManager.getInstance().getLastName(), "");
        tvName.setText(TextUtils.join(" ", new String[]{first, last}).trim());

        String email = SessionManager.getInstance().getEmail();
        tvEmail.setText(Objects.requireNonNullElse(email, ""));
        tvEmail.setVisibility(email != null ? View.VISIBLE : View.GONE);

        renderAvatar(SessionManager.getInstance().getAvatarUrl());
    }

    /**
     * Renders the given avatar URL (or the user's initials when {@code null}/blank) into the
     * header image, and wires a tap-to-enlarge listener via {@link #openFullscreenImage(String)}
     * whenever an actual avatar image is showing.
     *
     * @param avatarUrl the hosted avatar URL to render, or {@code null}/blank to show initials
     */
    private void renderAvatar(String avatarUrl) {
        GlideUtils.renderAvatarOrInitials(Glide.with(this), avatarUrl, ivAvatar, tvAvatarInitials,
                SessionManager.getInstance().getInitials());
        ivAvatar.setOnClickListener(avatarUrl == null || avatarUrl.isEmpty()
                ? null : v -> openFullscreenImage(avatarUrl));
    }

    /**
     * Opens {@link FullscreenImageActivity} to display the current avatar photo full-screen.
     *
     * @param imageUrl the avatar image's URL
     */
    private void openFullscreenImage(String imageUrl) {
        Intent intent = new Intent(this, FullscreenImageActivity.class);
        intent.putExtra(FullscreenImageActivity.EXTRA_IMAGE_URL, imageUrl);
        Navigator.start(this, intent);
    }

    /**
     * Wires the bottom navigation bar: marks the "Settings" tab selected, and navigates to the
     * matching top-level screen when another tab is tapped, clearing this screen off the back
     * stack in the process.
     */
    private void setupBottomNav() {
        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_settings);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_settings) return true;
            Class<? extends AppCompatActivity> target;
            if (id == R.id.nav_home) target = HomeActivity.class;
            else if (id == R.id.nav_my_recipes) target = MyRecipesActivity.class;
            else if (id == R.id.nav_favorites) target = FavoriteRecipesActivity.class;
            else return false;

            Intent extras = new Intent();
            extras.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            Navigator.start(this, target, extras);
            Navigator.finish(this);
            return true;
        });
    }

    /**
     * Binds every settings row's icon, label, subtitle, and click destination, and hides the
     * "Admin console" row entirely for non-admin accounts.
     */
    private void setupRows() {
        tvFavoritesSub = bindRow(R.id.row_favorites, R.drawable.ic_heart_filled,
                getString(R.string.settings_row_favorites_label),
                getString(R.string.settings_row_favorites_sub_format, 0),
                v -> Navigator.start(this, FavoriteRecipesActivity.class));

        tvMyRecipesSub = bindRow(R.id.row_my_recipes, R.drawable.ic_chef_hat,
                getString(R.string.settings_row_my_recipes_label),
                getString(R.string.settings_row_my_recipes_sub_format, 0),
                v -> Navigator.start(this, MyRecipesActivity.class));
        tvCookingSub = bindRow(R.id.row_cooking_preferences, R.drawable.ic_smartphone,
                getString(R.string.settings_row_cooking_preferences_label), null,
                v -> Navigator.start(this, CookingPreferencesActivity.class));
        refreshCookingPreferencesSub();

        bindRow(R.id.row_notification_preferences, R.drawable.ic_bell,
                getString(R.string.settings_row_notification_preferences_label), null,
                v -> Navigator.start(this, NotificationPreferencesActivity.class));

        bindRow(R.id.row_account_details, R.drawable.ic_user_cog,
                getString(R.string.settings_row_account_details_label), getString(R.string.settings_row_account_details_sub),
                v -> Navigator.start(this, AccountDetailsActivity.class));

        View adminRow = findViewById(R.id.row_admin_console);
        boolean isAdmin = SessionManager.getInstance().isAdmin();
        adminRow.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        if (isAdmin) {
            bindRow(R.id.row_admin_console, R.drawable.ic_shield,
                    getString(R.string.settings_row_admin_console_label), getString(R.string.settings_row_admin_console_sub),
                    v -> Navigator.start(this, AdminConsoleActivity.class));
        }

        LegalLinkSpanner.apply(findViewById(R.id.tv_legal_links), this, R.string.settings_footer_legal_links);
    }

    /**
     * Binds one {@code item_settings_row} include's icon, label, subtitle, and click listener.
     *
     * @param rowId the id of the {@code <include>} hosting the row
     * @param iconRes the row's icon drawable resource
     * @param label the row's bold label text
     * @param sub the row's subtitle text, or {@code null} to leave it for the caller to set later
     * @param onClick the action to run when the row is tapped
     * @return the row's subtitle {@link TextView}, allowing callers that need a dynamic subtitle
     *         to update it afterward
     */
    private TextView bindRow(int rowId, @DrawableRes int iconRes, String label, String sub,
                              View.OnClickListener onClick) {
        View row = findViewById(rowId);
        ((ImageView) row.findViewById(R.id.iv_row_icon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.tv_row_label)).setText(label);
        TextView tvSub = row.findViewById(R.id.tv_row_sub);
        if (sub != null) {
            tvSub.setText(sub);
        }
        row.setOnClickListener(onClick);
        return tvSub;
    }

    /**
     * Re-reads {@link CookingPreferencesStore} and updates the "Cooking preferences" row's
     * subtitle to match, since the toggles it summarizes are edited on
     * {@link CookingPreferencesActivity} and only become visible here once the user navigates
     * back.
     */
    private void refreshCookingPreferencesSub() {
        tvCookingSub.setText(CookingPreferencesStore.isScreenAwakeEnabled()
                ? R.string.settings_row_cooking_preferences_sub_on
                : R.string.settings_row_cooking_preferences_sub_off);
    }

    /**
     * Subscribes to the Favorites/My recipes count fetches, updating each row's subtitle once
     * its result resolves.
     */
    private void setupObservers() {
        viewModel.getFavoritesResult().observe(this, result -> {
            if (result instanceof ApiResult.Success<List<RecipePreviewResponse>> success) {
                tvFavoritesSub.setText(getString(R.string.settings_row_favorites_sub_format, success.getData().size()));
            }
        });

        viewModel.getMyRecipesResult().observe(this, result -> {
            if (result instanceof ApiResult.Success<List<RecipePreviewResponse>> success) {
                tvMyRecipesSub.setText(getString(R.string.settings_row_my_recipes_sub_format, success.getData().size()));
            }
        });
    }

    /**
     * Prompts for confirmation, then signs the user out via {@link SettingsViewModel#logout()}.
     */
    private void confirmLogout() {
        OrganicConfirmDialog.show(this, getString(R.string.settings_dialog_logout_title),
                getString(R.string.settings_dialog_logout_message), getString(R.string.settings_action_logout),
                getString(R.string.action_cancel), false, viewModel::logout);
    }
}
