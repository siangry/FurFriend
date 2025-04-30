package com.example.furfriend.screen.social;

import java.util.ArrayList;
import java.util.List;

public class Post {
    public String postId; // Firestore document ID
    public String userId; // ID of the post owner
    public String userName;
    public String contentText;
    public int imageResId;
    public String mediaUrl; // URL for media (image/video)
    public String dateTime;
    private long timestamp; // Added to store raw timestamp
    public boolean isLiked;
    public int likeCount;
    public int commentCount;
    public List<Comment> comments = new ArrayList<>();

    // Constructor for Firestore data
    public Post(String postId, String userId, String userName, String contentText, long timestamp, List<String> likes) {
        this.postId = postId;
        this.userId = userId;
        this.userName = userName;
        this.contentText = contentText;
        this.imageResId = 0; // Default; set to a drawable resource ID if needed
        this.mediaUrl = ""; // Default; fetch from Firestore if available
        this.timestamp = timestamp; // Store the raw timestamp
        this.dateTime = formatTimestamp(timestamp);
        this.isLiked = false; // Check if current user liked (implement later)
        this.likeCount = likes != null ? likes.size() : 0;
        this.commentCount = 0; // Fetch from Firestore if available
    }

    // Original constructor (for manual creation)
    public Post(String postId, String userId, String userName, String contentText, int imageResId, String mediaUrl, String dateTime) {
        this.postId = postId;
        this.userId = userId;
        this.userName = userName;
        this.contentText = contentText;
        this.imageResId = imageResId;
        this.mediaUrl = mediaUrl;
        this.timestamp = 0; // Default; set to 0 if not provided
        this.dateTime = dateTime;
        this.isLiked = false;
        this.likeCount = 0;
        this.commentCount = 0;
    }

    // Getters
    public String getPostId() { return postId; }
    public String getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getContentText() { return contentText; }
    public int getImageResId() { return imageResId; }
    public String getMediaUrl() { return mediaUrl; }
    public String getDateTime() { return dateTime; }
    public long getTimestamp() { return timestamp; } // Added getter for timestamp
    public boolean isLiked() { return isLiked; }
    public int getLikeCount() { return likeCount; }
    public int getCommentCount() { return commentCount; }
    public List<Comment> getComments() { return comments; }

    // Setters
    public void setLiked(boolean liked) { this.isLiked = liked; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public void setContentText(String contentText) { this.contentText = contentText; }
    public void setDateTime(String dateTime) { this.dateTime = dateTime; }
    public void setTimestamp(long timestamp) { // Added setter for timestamp
        this.timestamp = timestamp;
        this.dateTime = formatTimestamp(timestamp); // Update dateTime when timestamp changes
    }
    public void setMediaUrl(String mediaUrl) { // Added setter for mediaUrl
        this.mediaUrl = mediaUrl;
    }

    private String formatTimestamp(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("hh:mm a  MMMM dd, yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }
}