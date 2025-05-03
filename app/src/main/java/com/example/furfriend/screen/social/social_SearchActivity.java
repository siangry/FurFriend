package com.example.furfriend.screen.social;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.furfriend.BaseActivity;
import com.example.furfriend.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class social_SearchActivity extends BaseActivity {

    private static final String TAG = "SearchActivity";
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<social_user> filteredUserList;
    private EditText searchInput;
    private TextView emptyState;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private Handler searchHandler;
    private Runnable searchRunnable;
    private static final long SEARCH_DELAY_MS = 500; // Debounce delay in milliseconds
    private String lastQuery = ""; // Track the last query to ignore outdated results

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.social_search);

        // Initialize Firestore and Authentication
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Log.e(TAG, "User not authenticated");
            finish();
            return;
        }

        // Back Button
        ImageButton btnReturn = findViewById(R.id.btn_social_return);
        btnReturn.setOnClickListener(v -> finish());

        // Profile Button (shows current user's profile)
        ImageButton btnProfile = findViewById(R.id.btn_social_profile);
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, social_ProfileActivity.class);
            intent.putExtra("userId", currentUserId); // Use "userId" to match ProfileActivity
            startActivity(intent);
            finish();
        });

        // Notification Button
        ImageButton btnNotification = findViewById(R.id.btn_social_notification);
        btnNotification.setOnClickListener(v -> {
            Intent intent = new Intent(this, social_NotificationActivity.class);
            startActivity(intent);
            finish();
        });

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.recycler_social_search_results);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Initialize Empty State
        emptyState = findViewById(R.id.social_empty_state);
        emptyState.setVisibility(View.VISIBLE); // Show empty state initially

        // Initialize User List
        filteredUserList = new ArrayList<>();
        // Pass an OnUserClickListener to UserAdapter to handle clicks
        userAdapter = new UserAdapter(filteredUserList, user -> {
            Intent intent = new Intent(this, social_ProfileActivity.class);
            intent.putExtra("userId", user.getUid());
            startActivity(intent);
            finish();
        });
        recyclerView.setAdapter(userAdapter);

        // Initialize Handler for debouncing
        searchHandler = new Handler(Looper.getMainLooper());
        searchRunnable = () -> {
            String query = searchInput.getText().toString().trim();
            lastQuery = query; // Update the last query
            searchUsers(query);
        };

        // Search Input with Debouncing
        searchInput = findViewById(R.id.social_search_input);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Remove any pending search tasks
                searchHandler.removeCallbacks(searchRunnable);
                // Schedule a new search task after the delay
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
            }
        });
    }

    private void searchUsers(String query) {
        // If the query is empty, clear the list and show the empty state
        if (query.isEmpty()) {
            filteredUserList.clear();
            userAdapter.notifyDataSetChanged();
            emptyState.setVisibility(View.VISIBLE);
            return;
        }

        // Clear the list before starting a new search
        filteredUserList.clear();
        userAdapter.notifyDataSetChanged();

        // Fetch all user IDs from the users collection
        db.collection("users").get().addOnSuccessListener(querySnapshot -> {
            if (querySnapshot.isEmpty()) {
                userAdapter.notifyDataSetChanged();
                emptyState.setVisibility(View.VISIBLE);
                return;
            }

            List<DocumentSnapshot> userDocs = querySnapshot.getDocuments();
            AtomicInteger remaining = new AtomicInteger(userDocs.size());

            for (DocumentSnapshot document : userDocs) {
                String uid = document.getId();
                // Fetch the profile/userDetails document for each user
                db.collection("users").document(uid).collection("profile").document("userDetails").get()
                        .addOnSuccessListener(userDetailsDoc -> {
                            // Check if the query is still the latest one
                            if (!lastQuery.equals(query)) {
                                // Ignore results from outdated queries
                                return;
                            }

                            if (userDetailsDoc.exists()) {
                                String username = userDetailsDoc.getString("username");
                                String profileImageUrl = userDetailsDoc.getString("profilePictureBase64");

                                // Ensure username is not null before applying the filter
                                if (username != null && username.trim().toLowerCase().startsWith(query.toLowerCase())) {
                                    social_user user = new social_user(uid, username, profileImageUrl);
                                    // Avoid duplicates by checking if user is already in the list
                                    if (!filteredUserList.contains(user)) {
                                        filteredUserList.add(user);
                                    }
                                }
                            }

                            if (remaining.decrementAndGet() == 0) {
                                userAdapter.notifyDataSetChanged(); // Update UI once all users are processed
                                emptyState.setVisibility(filteredUserList.isEmpty() ? View.VISIBLE : View.GONE);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (remaining.decrementAndGet() == 0) {
                                userAdapter.notifyDataSetChanged();
                                emptyState.setVisibility(filteredUserList.isEmpty() ? View.VISIBLE : View.GONE);
                            }
                        });
            }
        }).addOnFailureListener(e -> {
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText("Error loading users");
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up Handler callbacks
        searchHandler.removeCallbacks(searchRunnable);
    }
}