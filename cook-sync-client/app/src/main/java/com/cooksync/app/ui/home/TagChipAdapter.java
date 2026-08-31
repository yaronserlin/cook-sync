package com.cooksync.app.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.cooksync.app.R;
import com.cooksync.app.ui.base.BaseAdapter;
import com.dtos.response.tags.TagResponse;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Client-layer (Android) {@code RecyclerView} adapter for a horizontal tag chip row, rendering
 * {@link TagResponse} DTOs fetched from the server's tag catalog endpoint. Used in two contexts:
 * as an interactive multi-select filter bar on {@link com.cooksync.app.ui.home.HomeActivity}
 * (with a leading "All" option that clears every selected tag), and as a read-only display of a
 * single recipe's own tags on {@link com.cooksync.app.ui.recipe.detail.RecipeDetailActivity} (no
 * "All" option, since there is nothing to filter there).
 *
 * @author Yaron Serlin
 * @version 1.3
 * @since 04/08/2026
 */
public class TagChipAdapter extends BaseAdapter<TagResponse, TagChipAdapter.ViewHolder> {

    private final boolean includeAllOption;
    private final Set<String> selectedTagNames = new HashSet<>();
    private OnTagClickListener listener;

    /** Notified when a chip in the row is tapped. */
    public interface OnTagClickListener {
        /** @param tagName the tapped chip's tag name, or {@code null} if the "All" chip was tapped */
        void onTagClick(String tagName);
    }

    /**
     * Creates an adapter that includes the leading "All" filter option (Home's usage).
     */
    public TagChipAdapter() {
        this(true);
    }

    /**
     * Creates an adapter with explicit control over whether a leading "All" option is
     * synthesized ahead of the real tags.
     *
     * @param includeAllOption {@code true} to prepend an "All" chip (filtering contexts),
     *                         {@code false} to show only the real tags (read-only display)
     */
    public TagChipAdapter(boolean includeAllOption) {
        this.includeAllOption = includeAllOption;
    }

    /** @param listener notified when a chip (including the synthetic "All" chip) is tapped */
    public void setOnTagClickListener(OnTagClickListener listener) {
        this.listener = listener;
    }

    /**
     * Replaces the tag set, prepending a synthetic "All" entry first when {@link #includeAllOption}
     * is set. That entry is a {@link TagResponse} with every field {@code null} — a sentinel
     * {@link #onBindViewHolder} recognizes via {@code tag.id() == null}, rather than a real tag —
     * so it renders the localized "All" label and clears the selection when tapped.
     *
     * @param newTags the full tag catalog to display, excluding the "All" entry
     */
    public void setTags(List<TagResponse> newTags) {
        List<TagResponse> combined = new ArrayList<>();
        if (includeAllOption) {
            combined.add(new TagResponse(null, null, null, null));
        }
        combined.addAll(newTags);
        setItems(combined);
    }

    /**
     * Replaces the highlighted set of selected tags (e.g. after applying the Filters sheet, or
     * toggling a chip directly). An empty set highlights the "All" chip instead, when present.
     *
     * @param tagNames the currently active tag selection
     */
    public void setSelectedTags(Set<String> tagNames) {
        selectedTagNames.clear();
        if (tagNames != null) {
            selectedTagNames.addAll(tagNames);
        }
        notifyDataSetChanged();
    }

    /**
     * Inflates a new tag chip view holder.
     *
     * @param parent the RecyclerView this chip is being added to
     * @param viewType the view type, unused (single chip layout)
     * @return the inflated view holder
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tag_chip, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds a tag's label and selected/unselected style to its chip view holder.
     *
     * @param holder the chip view holder to bind
     * @param position the tag's position in the adapter
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TagResponse tag = getItem(position);
        boolean isAllOption = tag.id() == null;

        if (isAllOption) {
            holder.tagName.setText(R.string.filter_all);
        } else {
            holder.tagName.setText(tag.name());
        }

        boolean isSelected = isAllOption
                ? selectedTagNames.isEmpty()
                : selectedTagNames.contains(tag.name());

        if (isSelected) {
            holder.card.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_accent));
            holder.tagName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_bg));
        } else {
            holder.card.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_neutral_300));
            holder.tagName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.color_text));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTagClick(tag.id() == null ? null : tag.name());
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView card;
        TextView tagName;

        ViewHolder(View view) {
            super(view);
            card = view.findViewById(R.id.chip_container);
            tagName = view.findViewById(R.id.tag_name);
        }
    }
}
