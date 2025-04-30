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
}