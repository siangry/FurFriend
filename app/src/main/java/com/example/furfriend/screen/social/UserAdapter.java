package com.example.furfriend.screen.social;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.furfriend.R;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<social_user> userList;
    private OnUserClickListener onUserClickListener;

    // Define the click listener interface
    public interface OnUserClickListener {
        void onUserClick(social_user user);
    }

    // Update constructor to accept the click listener
    public UserAdapter(List<social_user> userList, OnUserClickListener onUserClickListener) {
        this.userList = userList;
        this.onUserClickListener = onUserClickListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.social_item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        social_user user = userList.get(position);
        holder.tvUsername.setText(user.getUsername() != null ? user.getUsername() : "Unknown");

        // Load avatar using Glide with Base64 data
        String avatarDataUri = user.getProfileImageUrl() != null ? "data:image/jpeg;base64," + user.getProfileImageUrl() : null;
        Glide.with(holder.itemView.getContext())
                .load(avatarDataUri != null ? avatarDataUri : R.drawable.ic_social)
                .placeholder(R.drawable.ic_social)
                .error(R.drawable.ic_social)
                .into(holder.ivAvatar);

        // Set click listener on the entire item view
        holder.itemView.setOnClickListener(v -> {
            if (onUserClickListener != null) {
                onUserClickListener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar;
        TextView tvUsername;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.social_user_profile_image);
            tvUsername = itemView.findViewById(R.id.social_user_username);
        }
    }
}