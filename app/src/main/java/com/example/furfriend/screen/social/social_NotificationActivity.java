package com.example.furfriend.screen.social;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.furfriend.BaseActivity;
import com.example.furfriend.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.List;

public class social_NotificationActivity extends BaseActivity {

    private static final String TAG = "NotificationActivity";
    private RecyclerView recyclerView;
    private social_NotificationAdapter notificationAdapter;
    private List<social_Notification> notificationList;
    private TextView emptyState;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.social_notification);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Back Button
        ImageButton btnReturn = findViewById(R.id.btn_social_return);
        btnReturn.setOnClickListener(v -> finish());

        // Profile Button
        ImageButton btnProfile = findViewById(R.id.btn_social_profile);
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, social_ProfileActivity.class);
            intent.putExtra("userId", auth.getCurrentUser().getUid());
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
            // Already listening in real-time, no need to reload
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

        // Load notifications with real-time listener
        loadNotifications();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopListeningToNotifications();
    }

    private void loadNotifications() {
        String userId = auth.getCurrentUser().getUid(); // Assume user is signed in

        stopListeningToNotifications();
        notificationList.clear();
        notificationAdapter.notifyDataSetChanged();

        notificationListener = db.collection("notifications")
                .whereEqualTo("recipientId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading notifications", error);
                        emptyState.setVisibility(View.VISIBLE);
                        emptyState.setText("Error loading notifications");
                        return;
                    }
                    if (querySnapshot == null) return;

                    for (DocumentChange dc : querySnapshot.getDocumentChanges()) {
                        DocumentSnapshot document = dc.getDocument();
                        String id = document.getId();
                        String userIdSender = document.getString("userId");
                        String username = document.getString("username");
                        String type = document.getString("type");
                        String postId = document.getString("postId");
                        String content = document.getString("content");
                        Long timestamp = document.getLong("timestamp");
                        String avatarUrl = document.getString("avatarUrl");

                        // Log the avatarUrl to debug
                        Log.d(TAG, "Notification avatarUrl: " + (avatarUrl != null ? avatarUrl.substring(0, Math.min(50, avatarUrl.length())) + "..." : "null"));

                        // If avatarUrl is missing or empty, fetch it from the sender's profile
                        if (avatarUrl == null || avatarUrl.isEmpty()) {
                            if (userIdSender != null) {
                                db.collection("users").document(userIdSender).collection("profile").document("userDetails").get()
                                        .addOnSuccessListener(userDoc -> {
                                            String avatarBase64 = userDoc.getString("profilePictureBase64");
                                            String newAvatarUrl = avatarBase64 != null && !avatarBase64.isEmpty() ? "data:image/jpeg;base64," + avatarBase64 : "";
                                            Log.d(TAG, "Fetched avatar for user " + userIdSender + ": " + (newAvatarUrl.length() > 50 ? newAvatarUrl.substring(0, 50) + "..." : newAvatarUrl));

                                            // Update the notification object with the fetched avatar
                                            for (int i = 0; i < notificationList.size(); i++) {
                                                if (notificationList.get(i).getId().equals(id)) {
                                                    social_Notification updatedNotification = notificationList.get(i);
                                                    updatedNotification.setAvatarUrl(newAvatarUrl);
                                                    notificationList.set(i, updatedNotification);
                                                    notificationAdapter.notifyItemChanged(i);
                                                    break;
                                                }
                                            }
                                        })
                                        .addOnFailureListener(e -> Log.e(TAG, "Error fetching avatar for user: " + userIdSender, e));
                            }
                        }

                        social_Notification notification = new social_Notification(
                                id, userIdSender, username, type, postId, content,
                                timestamp != null ? timestamp : 0, avatarUrl
                        );

                        switch (dc.getType()) {
                            case ADDED:
                                int insertPosition = 0;
                                for (int i = 0; i < notificationList.size(); i++) {
                                    if (notificationList.get(i).getTimestamp() < notification.getTimestamp()) break;
                                    insertPosition++;
                                }
                                notificationList.add(insertPosition, notification);
                                notificationAdapter.notifyItemInserted(insertPosition);
                                break;
                            case MODIFIED:
                                for (int i = 0; i < notificationList.size(); i++) {
                                    if (notificationList.get(i).getId().equals(id)) {
                                        notificationList.set(i, notification);
                                        notificationAdapter.notifyItemChanged(i);
                                        break;
                                    }
                                }
                                break;
                            case REMOVED:
                                for (int i = 0; i < notificationList.size(); i++) {
                                    if (notificationList.get(i).getId().equals(id)) {
                                        notificationList.remove(i);
                                        notificationAdapter.notifyItemRemoved(i);
                                        break;
                                    }
                                }
                                break;
                        }
                    }

                    emptyState.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                    emptyState.setText(notificationList.isEmpty() ? "No notifications" : "");
                });
    }

    private void stopListeningToNotifications() {
        if (notificationListener != null) {
            notificationListener.remove();
            notificationListener = null;
        }
    }
}