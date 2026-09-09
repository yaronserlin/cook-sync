package com.cooksync.app.ui.recipe.importing;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.cooksync.app.R;
import com.cooksync.app.util.GlideUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal thumbnail strip of the local photos picked for a PHOTO recipe import, each removable
 * before the user starts the import — the order photos are added in is the order they're read in.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 05/09/2026
 */
public class RecipeImportPhotoAdapter extends RecyclerView.Adapter<RecipeImportPhotoAdapter.PhotoViewHolder> {

    /** Invoked when a thumbnail's remove button is tapped. */
    public interface OnRemoveListener {
        void onRemove(int position);
    }

    private final List<Uri> photoUris = new ArrayList<>();
    private OnRemoveListener onRemoveListener;

    public void setOnRemoveListener(OnRemoveListener listener) {
        this.onRemoveListener = listener;
    }

    /**
     * Replaces the displayed photo list.
     *
     * @param uris the picked photos' local URIs, in order
     */
    public void setPhotos(List<Uri> uris) {
        photoUris.clear();
        photoUris.addAll(uris);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recipe_import_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Uri uri = photoUris.get(position);
        GlideUtils.loadPreview(Glide.with(holder.itemView.getContext()), uri.toString(), holder.imageView);
        holder.removeButton.setOnClickListener(v -> {
            if (onRemoveListener != null) {
                onRemoveListener.onRemove(holder.getBindingAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return photoUris.size();
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        final ImageView imageView;
        final ImageButton removeButton;

        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.iv_import_photo);
            removeButton = itemView.findViewById(R.id.btn_remove_import_photo);
        }
    }
}
