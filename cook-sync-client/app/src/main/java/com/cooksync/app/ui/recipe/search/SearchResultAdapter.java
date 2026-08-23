package com.cooksync.app.ui.recipe.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cooksync.app.util.GlideUtils;
import com.cooksync.app.R;
import com.cooksync.app.ui.base.BaseAdapter;
import com.cooksync.app.util.RecipeFilterUtils;
import com.dtos.response.recipe.RecipePreviewResponse;

import java.util.List;

/**
 * Adapter for the dedicated {@link SearchActivity}'s result list. Renders the compact row
 * format the design specifies for search (76dp thumbnail + title/author/rating/time + chevron),
 * distinct from the elevated card format {@code RecipeCardAdapter} uses on the Home feed.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/08/2026
 */
public class SearchResultAdapter extends BaseAdapter<RecipePreviewResponse, SearchResultAdapter.ViewHolder> {

    private OnRecipeClickListener listener;

    public interface OnRecipeClickListener {
        void onRecipeClick(String recipeId);
    }

    public void setOnRecipeClickListener(OnRecipeClickListener listener) {
        this.listener = listener;
    }

    public void setRecipes(List<RecipePreviewResponse> newRecipes) {
        setItems(newRecipes);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_search_result_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RecipePreviewResponse recipe = getItem(position);

        holder.title.setText(recipe.title());
        holder.subtitle.setText(holder.itemView.getContext()
                .getString(R.string.search_result_subtitle_format, recipe.authorName(), recipe.reviewCount()));
        holder.rating.setText(RecipeFilterUtils.formatRating(recipe.averageRating()));
        holder.time.setText(holder.itemView.getContext()
                .getString(R.string.time_format, RecipeFilterUtils.totalTimeMinutes(recipe)));

        GlideUtils.loadThumbnail(Glide.with(holder.itemView.getContext()), recipe.primaryImageUrl(), holder.image);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecipeClick(recipe.id());
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title;
        TextView subtitle;
        TextView rating;
        TextView time;

        ViewHolder(View view) {
            super(view);
            image = view.findViewById(R.id.recipe_image);
            title = view.findViewById(R.id.recipe_title);
            subtitle = view.findViewById(R.id.recipe_subtitle);
            rating = view.findViewById(R.id.rating_text);
            time = view.findViewById(R.id.time_text);
        }
    }
}
