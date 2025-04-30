package com.example.furfriend.screen.social;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.furfriend.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class social_NotificationActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private social_NotificationAdapter notificationAdapter;
    private List<social_Notification> notificationList;
    private TextView emptyState;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.social_notification);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Back Button
        ImageButton btnReturn = findViewById(R.id.btn_social_return);
        btnReturn.setOnClickListener(v -> {
            finish();
        });

        // Profile Button
        ImageButton btnProfile = findViewById(R.id.btn_social_profile);
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, social_ProfileActivity.class);
            startActivity(intent);
            finish();
        });

        // Search Button
        ImageButton btnSearch = findViewById(R.id.btn_social_search);
        btnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(this, social_SearchActivity.class);
            startActivity(intent);
            finish();
        });

        // Notification Button
        ImageButton btnNotification = findViewById(R.id.btn_social_notification);
        btnNotification.setOnClickListener(v -> {
            // refresh notifications
            loadNotifications();
        });

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recycler_social_notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize Empty State
        emptyState = findViewById(R.id.empty_state);
        emptyState.setVisibility(View.GONE);

        // Initialize Notification List
        notificationList = new ArrayList<>();
        notificationAdapter = new social_NotificationAdapter(notificationList);
        recyclerView.setAdapter(notificationAdapter);

        addDummyNotifications();

        // Load notifications
        loadNotifications();
    }

    private void loadNotifications() {
        String userId = auth.getCurrentUser().getUid(); // Assume user is signed in

        db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    notificationList.clear();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String id = document.getId();
                        String userIdSender = document.getString("userId");
                        String username = document.getString("username");
                        String type = document.getString("type");
                        String postId = document.getString("postId");
                        String content = document.getString("content");
                        Long timestamp = document.getLong("timestamp");
                        String avatarUrl = document.getString("avatarUrl");

                        notificationList.add(new social_Notification(
                                id, userIdSender, username, type, postId, content, timestamp != null ? timestamp : 0, avatarUrl
                        ));
                    }
                    notificationAdapter.notifyDataSetChanged();
                    emptyState.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                    emptyState.setText("No notifications");
                })
                .addOnFailureListener(e -> {
                    String errorMessage = "Error loading notifications" + e.getMessage();
                    emptyState.setVisibility(View.VISIBLE);
                    emptyState.setText(errorMessage);
                });
    }

    private void addDummyNotifications() {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "test_user";
        db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        // Dummy notification 1: Like
                        Map<String, Object> notification1 = new HashMap<>();
                        notification1.put("userId", "UyPRrfNYIpY7UrDYdtIFzfrVg093");
                        notification1.put("username", "Alice");
                        notification1.put("type", "like");
                        notification1.put("postId", "post123");
                        notification1.put("content", "Alice liked your post");
                        notification1.put("timestamp", 1730247600000L); // 10 mins ago (Apr 29, 2025, 10:00 AM UTC)
                        notification1.put("avatarUrl", "https://example.com/images/alice.jpg");
                        notification1.put("recipientId", userId);

                        // Dummy notification 2: Follow
                        Map<String, Object> notification2 = new HashMap<>();
                        notification2.put("userId", "uid2");
                        notification2.put("username", "Bob");
                        notification2.put("type", "follow");
                        notification2.put("postId", "");
                        notification2.put("content", "Bob followed you");
                        notification2.put("timestamp", 1730161200000L); // 1 day ago (Apr 28, 2025, 10:00 AM UTC)
                        notification2.put("avatarUrl", "");
                        notification2.put("recipientId", userId);

                        // Dummy notification 3: Comment
                        Map<String, Object> notification3 = new HashMap<>();
                        notification3.put("userId", "yfI8CeGfu9cnuQnRFeDCRjU9str2");
                        notification3.put("username", "Charlie");
                        notification3.put("type", "comment");
                        notification3.put("postId", "post124");
                        notification3.put("content", "Charlie commented on your post");
                        notification3.put("timestamp", 1729642800000L); // 1 week ago (Apr 22, 2025, 10:00 AM UTC)
                        notification3.put("avatarUrl", "https://example.com/images/charlie.jpg");
                        notification3.put("recipientId", userId);

                        // Add notifications to Firestore
                        db.collection("notifications").add(notification1);
                        //db.collection("notifications").add(notification2);
                        db.collection("notifications").add(notification3);}
                });
    }
}