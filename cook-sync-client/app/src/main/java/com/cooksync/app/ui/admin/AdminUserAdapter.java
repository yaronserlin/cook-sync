package com.cooksync.app.ui.admin;
import com.cooksync.app.ui.base.Navigator;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cooksync.app.R;
import com.cooksync.app.ui.base.BaseAdapter;
import com.cooksync.app.ui.common.AvatarView;
import com.cooksync.app.util.UserNameFormatter;
import com.dtos.response.user.UserResponse;
import com.google.android.material.button.MaterialButton;

import java.util.List;

/**
 * Adapter for the Admin Console's Users tab: renders each account row with its status tag and
 * enable/disable toggle.
 *
 * @author Yaron Serlin
 * @version 1.0
 * @since 07/08/2026
 */
public class AdminUserAdapter extends BaseAdapter<UserResponse, AdminUserAdapter.ViewHolder> {

    /** Notified when the moderator acts on a user row. */
    public interface OnUserActionListener {
        /**
         * @param user the row's user
         * @param enabled the new enabled state requested (opposite of the user's current one)
         */
        void onToggleEnabled(UserResponse user, boolean enabled);

        /** @param user the row that was long-pressed, to start the permanent-delete flow */
        void onDeleteUser(UserResponse user);
    }

    private OnUserActionListener listener;

    /**
     * Replaces the displayed user list.
     *
     * @param newUsers the complete user list to display
     */
    public void setUsers(List<UserResponse> newUsers) {
        setItems(newUsers);
    }

    /**
     * Sets the listener notified of row actions.
     *
     * @param listener the listener to notify, or {@code null} to detach
     */
    public void setOnUserActionListener(OnUserActionListener listener) {
        this.listener = listener;
    }

    /**
     * Inflates a new user row view holder.
     *
     * @param parent the RecyclerView this row is being added to
     * @param viewType the view type, unused (single row layout)
     * @return the inflated view holder
     */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(view);
    }

    /**
     * Binds a user's identity, status tag, and enable/disable toggle to its row view holder.
     *
     * @param holder the row view holder to bind
     * @param position the user's position in the adapter
     */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserResponse user = getItem(position);
        String fullName = UserNameFormatter.fullName(user.firstName(), user.lastName());

        holder.name.setText(fullName);
        holder.avatar.setAvatar(user.avatarUrl(), fullName);

        View.OnClickListener openProfile = v -> {
            if (user.id() != null && v.getContext() instanceof android.app.Activity activity) {
                android.content.Intent intent = new android.content.Intent();
                intent.putExtra(com.cooksync.app.ui.auth.UserProfileActivity.EXTRA_USER_ID, user.id());
                intent.putExtra(com.cooksync.app.ui.auth.UserProfileActivity.EXTRA_USER_NAME, fullName);
                com.cooksync.app.ui.base.Navigator.start(activity, com.cooksync.app.ui.auth.UserProfileActivity.class, intent);
            }
        };
        holder.avatar.setOnClickListener(openProfile);
        holder.name.setOnClickListener(openProfile);

        holder.email.setText(user.email());
        holder.adminTag.setVisibility(user.isAdmin() ? View.VISIBLE : View.GONE);

        Resources resources = holder.itemView.getResources();
        if (AdminUsersViewModel.STATUS_SUSPENDED.equals(user.status())) {
            holder.status.setText(R.string.admin_user_status_suspended);
            holder.status.setBackgroundTintList(ColorStateList.valueOf(
                    resources.getColor(R.color.color_accent_200, null)));
            holder.status.setTextColor(resources.getColor(R.color.color_accent_800, null));
        } else if (AdminUsersViewModel.STATUS_DEACTIVATED.equals(user.status())) {
            holder.status.setText(R.string.admin_user_status_deactivated);
            holder.status.setBackgroundTintList(ColorStateList.valueOf(
                    resources.getColor(R.color.color_neutral_300, null)));
            holder.status.setTextColor(resources.getColor(R.color.color_neutral_800, null));
        } else {
            holder.status.setText(R.string.admin_user_status_active);
            holder.status.setBackgroundTintList(ColorStateList.valueOf(
                    resources.getColor(R.color.color_accent_2_200, null)));
            holder.status.setTextColor(resources.getColor(R.color.color_accent_2_800, null));
        }
        if (user.enabled()) {
            holder.toggleEnabled.setText(R.string.action_suspend);
            int danger = resources.getColor(R.color.color_danger, null);
            holder.toggleEnabled.setStrokeColor(ColorStateList.valueOf(danger));
            holder.toggleEnabled.setTextColor(danger);
        } else {
            holder.toggleEnabled.setText(R.string.action_reactivate);
            int success = resources.getColor(R.color.color_success, null);
            holder.toggleEnabled.setStrokeColor(ColorStateList.valueOf(success));
            holder.toggleEnabled.setTextColor(success);
        }

        holder.toggleEnabled.setOnClickListener(v -> {
            if (listener != null) listener.onToggleEnabled(user, !user.enabled());
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onDeleteUser(user);
            return true;
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        AvatarView avatar;
        TextView name;
        TextView adminTag;
        TextView email;
        TextView status;
        MaterialButton toggleEnabled;

        ViewHolder(View view) {
            super(view);
            avatar = view.findViewById(R.id.user_avatar);
            name = view.findViewById(R.id.tv_user_name);
            adminTag = view.findViewById(R.id.tv_user_admin_tag);
            email = view.findViewById(R.id.tv_user_email);
            status = view.findViewById(R.id.tv_user_status);
            toggleEnabled = view.findViewById(R.id.btn_user_toggle_enabled);
        }
    }
}
