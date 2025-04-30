package com.example.furfriend.screen.social;

public class social_user {
    private String uid; // Firebase user ID
    private String username;
    private String profileImageUrl; // URL for profile image (optional)

    public social_user(String uid, String username, String profileImageUrl) {
        this.uid = uid;
        this.username = username;
        this.profileImageUrl = profileImageUrl != null ? profileImageUrl : "";
    }

    public String getUid() {
        return uid;
    }

    public String getUsername() {
        return username;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }
}
