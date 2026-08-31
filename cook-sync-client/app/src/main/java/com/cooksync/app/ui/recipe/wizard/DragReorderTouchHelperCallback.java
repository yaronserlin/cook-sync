package com.cooksync.app.ui.recipe.wizard;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Shared up/down-only drag-reorder {@link ItemTouchHelper.Callback}, used by both the
 * Ingredients and Instructions steps of the Create Recipe wizard. Dragging starts only from a
 * row's drag-handle icon (see {@code setOnTouchListener} + {@code ItemTouchHelper#startDrag} in
 * each adapter), not from a long-press anywhere on the row, since the row also contains text
 * fields that need normal touch/focus behavior.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 08/08/2026
 */
public class DragReorderTouchHelperCallback extends ItemTouchHelper.Callback {

    /** Notified as rows are dragged past each other, and once the drag finishes. */
    public interface OnMoveListener {
        /**
         * Invoked as the dragged row passes another row's position; the caller is expected to
         * move the underlying item and notify the adapter immediately, since this can fire
         * repeatedly during a single drag.
         *
         * @param fromPosition the dragged row's current adapter position
         * @param toPosition the position it's being dragged past
         */
        void onMove(int fromPosition, int toPosition);

        /** Called once the user releases the dragged row. Default no-op. */
        default void onDragFinished() {
        }
    }

    private final OnMoveListener listener;

    /**
     * @param listener the listener notified of row moves and drag completion
     */
    public DragReorderTouchHelperCallback(OnMoveListener listener) {
        this.listener = listener;
    }

    /** @return {@code false}; dragging only starts from a row's drag handle, never a long-press */
    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    /** @return {@code false}; these lists don't support swipe-to-dismiss */
    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    /**
     * @param recyclerView the hosting RecyclerView, unused
     * @param viewHolder the row being queried, unused (every row allows the same movement)
     * @return movement flags allowing only up/down drag, no swipe
     */
    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
    }

    /**
     * Forwards a drag-past event to {@link #listener}.
     *
     * @param recyclerView the hosting RecyclerView, unused
     * @param viewHolder the row being dragged
     * @param target the row it's currently being dragged past
     * @return {@code true}, indicating the move was handled
     */
    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder,
                           @NonNull RecyclerView.ViewHolder target) {
        listener.onMove(viewHolder.getBindingAdapterPosition(), target.getBindingAdapterPosition());
        return true;
    }

    /**
     * No-op: swiping is disabled via {@link #isItemViewSwipeEnabled()}, so this is never invoked.
     *
     * @param viewHolder the swiped row, unused
     * @param direction the swipe direction, unused
     */
    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
    }

    /**
     * Notifies {@link #listener} that the drag has finished, once the released row settles back
     * into place.
     *
     * @param recyclerView the hosting RecyclerView
     * @param viewHolder the row that was being dragged
     */
    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        listener.onDragFinished();
    }
}
