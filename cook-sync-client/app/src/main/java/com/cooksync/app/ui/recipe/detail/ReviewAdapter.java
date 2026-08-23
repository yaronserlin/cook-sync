/**
 * Client-layer (Android) component of the Reviews feature. RecyclerView adapter rendering
 * {@code ReviewResponse} DTOs (as returned by the server's {@code ReviewController} and embedded
 * by {@code RecipeMapper}) within Recipe Detail's reviews list; its overflow menu drives
 * {@code RecipeDetailViewModel.deleteReview}/{@code reportReview}.
 */
package com.cooksync.app.ui.recipe.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cooksync.app.R;
import com.cooksync.app.ui.base.BaseAdapter;
import com.cooksync.app.ui.common.AvatarView;
import com.cooksync.app.util.DateFormatUtils;
import com.dtos.response.review.ReviewResponse;

import java.util.List;

/**
 * Adapter for the recipe reviews list.
 *
 * @author Yaron Serlin
 * @version 1.2
 * @since 04/08/2026
 */
public class ReviewAdapter extends BaseAdapter<ReviewResponse, ReviewAdapter.ViewHolder> {

    /** Notified when the viewer chooses an action from a review's overflow menu. */
    public interface OnReviewActionListener {
        /**
         * @param review the review the viewer, who is its author, chose to delete
         */
        void onDeleteReview(ReviewResponse review);

        /**
         * @param review the review the viewer, who is not its author, chose to report
         */
        void onReportReview(ReviewResponse review);
    }

    /** Notified when the viewer taps a review author's avatar to view it full-screen. */
    public interface OnAvatarClickListener {
        /**
         * @param avatarUrl the tapped author's avatar URL
         */
        void onAvatarClick(String avatarUrl);
    }

    /** Notified when the viewer taps a review author's name/avatar to open their profile. */
    public interface OnAuthorClickListener {
        /**
         * @param userId the tapped author's user ID
         * @param authorName the tapped author's display name
         */
        void onAuthorClick(String userId, String authorName);
    }

    private String currentUserId;
    private OnReviewActionListener actionListener;
    private OnAvatarClickListener avatarClickListener;
    private OnAuthorClickListener authorClickListener;

    /**
     * Replaces the adapter's backing list with a new set of reviews to display.
     *
     * @param newReviews the reviews to display, in display order
     */
    public void setReviews(List<ReviewResponse> newReviews) {
        setItems(newReviews);
    }

    /**
     * Sets the signed-in viewer's user ID, used to decide whether each review's overflow menu
     * offers "Delete" (viewer is the author) or "Report" (viewer is not).
     *
     * @param currentUserId the signed-in user's ID, or {@code null} if signed out
     */
    public void setCurrentUserId(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    /**
     * @param listener callback notified when the viewer deletes or reports a review
     */
    public void setOnReviewActionListener(OnReviewActionListener listener) {
        this.actionListener = listener;
    }

    /**
     * @param listener callback notified when the viewer taps a review author's avatar
     */
    public void setOnAvatarClickListener(OnAvatarClickListener listener) {
        this.avatarClickListener = listener;
    }

    /**
     * @param listener callback notified when the viewer taps a review author's name or avatar
     */
    public void setOnAuthorClickListener(OnAuthorClickListener listener) {
        this.authorClickListener = listener;
    }

    /**
     * Inflates the review row layout and wraps it in a new {@link ViewHolder}.
     *
     * @param parent the RecyclerView the row is being attached to
     * @param viewType the view type, unused (this adapter has a single row layout)
     * @return a new, unbound {@link ViewHolder}
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds one review's author, rating, title, comment, and relative date onto the row, and
     * wires the overflow menu with the correct Delete/Report action for the signed-in viewer.
     *
     * @param holder the row's view holder
     * @param position the review's position in the adapter's backing list
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReviewResponse review = getItem(position);

        String author = review.authorName();
        holder.authorName.setText(author);
        holder.avatar.setAvatar(review.authorAvatarUrl(), author);
        View.OnClickListener openProfile = v -> {
            if (review.userId() != null && authorClickListener != null) {
                authorClickListener.onAuthorClick(review.userId(), author);
            }
        };
        holder.avatar.setOnClickListener(openProfile);
        holder.authorName.setOnClickListener(openProfile);

        holder.rating.setText(review.rating() != null ? review.rating().toString() : "0.0");
        holder.title.setText(review.title());
        holder.content.setText(review.comment());
        holder.date.setText(DateFormatUtils.formatRelativeDay(review.createdAt()));

        boolean isAuthor = currentUserId != null && currentUserId.equals(review.userId());
        holder.overflow.setOnClickListener(v -> showOverflowMenu(v, review, isAuthor));
    }

    /**
     * Shows the row's overflow popup with a single action — "Delete" if the viewer authored the
     * review, "Report" otherwise — and forwards the choice to {@link #actionListener}.
     *
     * @param anchor the overflow button the popup is anchored to
     * @param review the review the popup's action applies to
     * @param isAuthor {@code true} if the signed-in viewer authored {@code review}
     */
    private void showOverflowMenu(View anchor, ReviewResponse review, boolean isAuthor) {
        PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
        popup.getMenu().add(0, 1, 0, isAuthor ? anchor.getContext().getString(R.string.action_delete) : anchor.getContext().getString(R.string.action_report));
        popup.setOnMenuItemClickListener(item -> {
            if (actionListener == null) {
                return true;
            }
            if (isAuthor) {
                actionListener.onDeleteReview(review);
            } else {
                actionListener.onReportReview(review);
            }
            return true;
        });
        popup.show();
    }

    /**
     * View holder caching the review row's child view references.
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        AvatarView avatar;
        TextView authorName;
        TextView date;
        TextView rating;
        TextView title;
        TextView content;
        ImageButton overflow;

        /**
         * Resolves every child view reference from the inflated review row.
         *
         * @param view the inflated {@code item_review} row root view
         */
        ViewHolder(View view) {
            super(view);
            avatar = view.findViewById(R.id.review_author_avatar);
            authorName = view.findViewById(R.id.review_author_name);
            date = view.findViewById(R.id.review_date);
            rating = view.findViewById(R.id.review_rating);
            title = view.findViewById(R.id.review_title);
            content = view.findViewById(R.id.review_content);
            overflow = view.findViewById(R.id.btn_review_overflow);
        }
    }
}
