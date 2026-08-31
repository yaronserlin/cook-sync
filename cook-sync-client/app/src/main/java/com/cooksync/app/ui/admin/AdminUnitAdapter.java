package com.cooksync.app.ui.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cooksync.app.R;
import com.cooksync.app.ui.base.BaseAdapter;
import com.dtos.response.unit.UnitResponse;

import java.util.List;

/**
 * Adapter for displaying measurement units in the Admin Console.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 10/08/2026
 */
public class AdminUnitAdapter extends BaseAdapter<UnitResponse, AdminUnitAdapter.ViewHolder> {

    /** Notified when the moderator taps a unit row's delete action. */
    public interface Listener {
        /** @param unit the row whose deletion was requested */
        void onDeleteUnit(UnitResponse unit);
    }

    private Listener listener;

    /**
     * Replaces the displayed unit list.
     *
     * @param newUnits the complete unit list to display
     */
    public void setUnits(List<UnitResponse> newUnits) {
        setItems(newUnits);
    }

    /**
     * Removes a single unit row, e.g. after a successful delete.
     *
     * @param unit the unit to remove
     */
    public void removeUnit(UnitResponse unit) {
        removeItem(unit);
    }

    /**
     * Re-inserts a previously removed unit row, e.g. after an undone delete.
     *
     * @param unit the unit to restore
     */
    public void restoreUnit(UnitResponse unit) {
        addItem(unit);
    }

    /**
     * Sets the listener notified of row actions.
     *
     * @param listener the listener to notify, or {@code null} to detach
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /**
     * Inflates a new unit row view holder.
     *
     * @param parent the RecyclerView this row is being added to
     * @param viewType the view type, unused (single row layout)
     * @return the inflated view holder
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_unit, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds a unit's code, name, and delete action to its row view holder.
     *
     * @param holder the row view holder to bind
     * @param position the unit's position in the adapter
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UnitResponse unit = getItem(position);
        holder.tvCode.setText(unit.code());
        holder.tvName.setText(unit.name());
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteUnit(unit);
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCode;
        TextView tvName;
        ImageButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCode = itemView.findViewById(R.id.tv_unit_code);
            tvName = itemView.findViewById(R.id.tv_unit_name);
            btnDelete = itemView.findViewById(R.id.btn_delete_unit);
        }
    }
}
