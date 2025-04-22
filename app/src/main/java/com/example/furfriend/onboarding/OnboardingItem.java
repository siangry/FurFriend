package com.example.furfriend.onboarding;

public class OnboardingItem {
    int imageId;
    int indicatorId;
    String title, description;

    public OnboardingItem(int imageId, int indicatorId, String title, String description) {
        this.imageId = imageId;
        this.indicatorId = indicatorId;
        this.title = title;
        this.description = description;
    }
}
