package com.example.furfriend.screen.social;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.furfriend.R;
import java.util.List;

public class social_NotificationAdapter extends RecyclerView.Adapter<social_NotificationAdapter.NotificationViewHolder> {

    private List<social_Notification> notificationList;

    public social_NotificationAdapter(List<social_Notification> notificationList) {
        this.notificationList = notificationList;
    }

    @Override
    public NotificationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.social_item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(NotificationViewHolder holder, int position) {
        social_Notification notification = notificationList.get(position);
        holder.contentTextView.setText(notification.getContent());

        // Format time ago
        CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                notification.getTimestamp(),
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
        );
        holder.timeTextView.setText(timeAgo);

        // Load avatar with Glide
        if (notification.getAvatarUrl() != null && !notification.getAvatarUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(notification.getAvatarUrl())
                    .placeholder(R.drawable.ic_social_profile_icon)
                    .error(R.drawable.ic_social_profile_icon)
                    .into(holder.avatarImageView);
        } else {
            holder.avatarImageView.setImageResource(R.drawable.ic_social_profile_icon);
        }
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImageView;
        TextView contentTextView;
        TextView timeTextView;

        NotificationViewHolder(View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.notification_avatar);
            contentTextView = itemView.findViewById(R.id.notification_content);
            timeTextView = itemView.findViewById(R.id.notification_time);
        }
    }
}