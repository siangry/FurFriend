package com.example.furfriend.screen.social;

import com.example.furfriend.BaseActivity;

public class social_Notification extends BaseActivity {
    private String id; // Firestore document ID
    private String userId; // ID of the user who triggered the notification
    private String username; // Username of the user who triggered the notification
    private String type; // "like", "follow", or "comment"
    private String postId; // ID of the post (for likes/comments), nullable for follows
    private String content; // Notification message (e.g., "Alice liked your post")
    private long timestamp; // Timestamp in milliseconds
    private String avatarUrl; // URL for the user’s avatar (optional)

    public social_Notification(String id, String userId, String username, String type, String postId, String content, long timestamp, String avatarUrl) {
        this.id = id;
        this.userId = userId;
        this.username = username;
        this.type = type;
        this.postId = postId;
        this.content = content;
        this.timestamp = timestamp;
        this.avatarUrl = avatarUrl != null ? avatarUrl : "";
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getType() {
        return type;
    }

    public String getPostId() {
        return postId;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl != null ? avatarUrl : "";
    }
}