package com.cooksync.app.ui.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cooksync.app.R;
import com.cooksync.app.ui.base.BaseAdapter;
import com.cooksync.app.util.DateFormatUtils;
import com.dtos.response.announcement.AnnouncementResponse;

import java.util.List;

/**
 * Adapter for displaying past system announcements in the Admin Console, each with a
 * "Deactivate" action shown only while it's still active.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 04/09/2026
 */
public class AdminAnnouncementAdapter extends BaseAdapter<AnnouncementResponse, AdminAnnouncementAdapter.ViewHolder> {

    /** Notified when the moderator taps a still-active announcement's deactivate action. */
    public interface Listener {
        /** @param announcement the row whose deactivation was requested */
        void onDeactivate(AnnouncementResponse announcement);
    }

    private Listener listener;

    /**
     * Replaces the displayed announcement list.
     *
     * @param newAnnouncements the complete, newest-first announcement list to display
     */
    public void setAnnouncements(List<AnnouncementResponse> newAnnouncements) {
        setItems(newAnnouncements);
    }

    /**
     * Sets the listener notified of row actions.
     *
     * @param listener the listener to notify, or {@code null} to detach
     */
    public void setListener(Listener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_announcement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnnouncementResponse announcement = getItem(position);
        Context context = holder.itemView.getContext();

        holder.tvTitle.setText(announcement.title());
        holder.tvBody.setText(announcement.body());
        holder.tvDate.setText(DateFormatUtils.formatRelativeDay(announcement.createdAt()));

        boolean actionRequired = "ACTION_REQUIRED".equals(announcement.severity());
        holder.tvSeverity.setText(actionRequired
                ? R.string.admin_announcements_severity_action_required
                : R.string.admin_announcements_severity_info);
        holder.tvSeverity.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                context.getColor(actionRequired ? R.color.color_danger : R.color.color_accent)));

        if (announcement.active()) {
            holder.tvStatus.setText(R.string.admin_announcements_status_active);
            holder.btnDeactivate.setVisibility(View.VISIBLE);
            holder.btnDeactivate.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeactivate(announcement);
                }
            });
        } else {
            holder.tvStatus.setText(R.string.admin_announcements_status_inactive);
            holder.btnDeactivate.setVisibility(View.GONE);
            holder.btnDeactivate.setOnClickListener(null);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvBody;
        TextView tvSeverity;
        TextView tvStatus;
        TextView tvDate;
        Button btnDeactivate;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_announcement_title);
            tvBody = itemView.findViewById(R.id.tv_announcement_body);
            tvSeverity = itemView.findViewById(R.id.tv_announcement_severity);
            tvStatus = itemView.findViewById(R.id.tv_announcement_status);
            tvDate = itemView.findViewById(R.id.tv_announcement_date);
            btnDeactivate = itemView.findViewById(R.id.btn_deactivate_announcement);
        }
    }
}
