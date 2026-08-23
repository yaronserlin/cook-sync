package com.cooksync.app.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cooksync.app.R;
import com.cooksync.app.ui.base.BaseAdapter;
import com.cooksync.app.util.RecipeFilterUtils;
import com.dtos.response.recipe.RecipePreviewResponse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Client-layer (Android) {@code RecyclerView} adapter rendering the Home feed's recipe cards
 * from {@link RecipePreviewResponse} DTOs shared with the server — the same preview shape used
 * across the feed, search results, and recipe-row lists. Renders the app's high-fidelity card
 * format (large image, title, author, blurb, difficulty/rating/time, and a favorite toggle),
 * distinct from the compact row format {@code SearchResultAdapter}/{@code RecipeRowCardAdapter}
 * use elsewhere.
 *
 * @author Yaron Serlin
 * @version 1.1
 * @since 04/08/2026
 */
public class RecipeCardAdapter extends BaseAdapter<RecipePreviewResponse, RecipeCardAdapter.ViewHolder> {

    private final Set<String> favoriteIds = new HashSet<>();
    private OnRecipeClickListener listener;

    /** Notified of row taps and favorite-icon taps on the bound recipe cards. */
    public interface OnRecipeClickListener {
        /** @param recipeId the tapped card's recipe id */
        void onRecipeClick(String recipeId);

        /**
         * @param recipeId the tapped card's recipe id
         * @param wasFavorite whether the recipe was already favorited before this tap (i.e.
         *                    {@code false} means this tap is adding it, {@code true} means
         *                    it's removing it)
         */
        void onFavoriteClick(String recipeId, boolean wasFavorite);
    }

    /** @param listener notified of row taps and favorite-icon taps */
    public void setOnRecipeClickListener(OnRecipeClickListener listener) {
        this.listener = listener;
    }

    /** @param newRecipes the full replacement feed page/accumulated list to render */
    public void setRecipes(List<RecipePreviewResponse> newRecipes) {
        setItems(newRecipes);
    }

    /**
     * Replaces the set of recipe ids considered favorited, used to render each card's heart
     * icon filled or outlined, then refreshes every bound row.
     *
     * @param favorites the user's current favorites, as returned by {@code GET /api/favorites}
     */
    public void setFavorites(List<RecipePreviewResponse> favorites) {
        favoriteIds.clear();
        for (RecipePreviewResponse favorite : favorites) {
            favoriteIds.add(favorite.id());
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecipePreviewResponse recipe = getItem(position);

        holder.title.setText(recipe.title());
        holder.author.setText(recipe.authorName());
        holder.blurb.setText(recipe.description());
        holder.difficulty.setText(recipe.difficulty());
        holder.rating.setText(RecipeFilterUtils.formatRating(recipe.averageRating()));
        holder.time.setText(holder.itemView.getContext().getString(R.string.time_format, RecipeFilterUtils.totalTimeMinutes(recipe)));

        Glide.with(holder.itemView.getContext())
                .load(recipe.primaryImageUrl())
                .placeholder(R.drawable.bg_skeleton_bone)
                .error(R.drawable.ic_image_failed)
                .centerCrop()
                .into(holder.image);

        boolean isFavorite = favoriteIds.contains(recipe.id());
        holder.btnFavorite.setImageResource(isFavorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeClick(recipe.id());
            }
        });

        holder.btnFavorite.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFavoriteClick(recipe.id(), isFavorite);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        ImageButton btnFavorite;
        TextView difficulty;
        TextView title;
        TextView author;
        TextView blurb;
        TextView rating;
        TextView time;

        ViewHolder(View view) {
            super(view);
            image = view.findViewById(R.id.recipe_image);
            btnFavorite = view.findViewById(R.id.btn_favorite);
            difficulty = view.findViewById(R.id.difficulty_text);
            title = view.findViewById(R.id.recipe_title);
            author = view.findViewById(R.id.author_name);
            blurb = view.findViewById(R.id.recipe_blurb);
            rating = view.findViewById(R.id.rating_text);
            time = view.findViewById(R.id.time_text);
        }
    }
}
