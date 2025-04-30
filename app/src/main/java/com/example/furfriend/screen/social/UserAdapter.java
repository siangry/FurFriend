package com.example.furfriend.screen.social;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.furfriend.R;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<social_user> userList;

    public UserAdapter(List<social_user> userList) {
        this.userList = userList;
    }

    @Override
    public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.social_item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(UserViewHolder holder, int position) {
        social_user user = userList.get(position);
        holder.usernameTextView.setText(user.getUsername());

        // Load profile image with Glide
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_social_profile_icon) // Default image
                    .error(R.drawable.ic_social_profile_icon)
                    .into(holder.profileImageView);
        } else {
            holder.profileImageView.setImageResource(R.drawable.ic_social_profile_icon);
        }

        // Navigate to user profile on click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(holder.itemView.getContext(), social_ProfileActivity.class);
            intent.putExtra("uid", user.getUid());
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        ImageView profileImageView;
        TextView usernameTextView;

        UserViewHolder(View itemView) {
            super(itemView);
            profileImageView = itemView.findViewById(R.id.social_user_profile_image);
            usernameTextView = itemView.findViewById(R.id.social_user_username);
        }
    }
}