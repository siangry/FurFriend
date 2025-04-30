package com.example.furfriend.screen.social;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.furfriend.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class social_SearchActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<social_user> filteredUserList;
    private EditText searchInput;
    private TextView emptyState;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.social_search);

        // Initialize Firestore and Authentication
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            finish();
            return;
        }

        // Back Button
        ImageButton btnReturn = findViewById(R.id.btn_social_return);
        btnReturn.setOnClickListener(v -> finish());

        // Profile Button
        ImageButton btnProfile = findViewById(R.id.btn_social_profile);
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, social_ProfileActivity.class);
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
        userAdapter = new UserAdapter(filteredUserList);
        recyclerView.setAdapter(userAdapter);

        // Search Input
        searchInput = findViewById(R.id.social_search_input);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchUsers(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchUsers(String query) {
        // If the query is empty, clear the list and show the empty state
        if (query.trim().isEmpty()) {
            filteredUserList.clear();
            userAdapter.notifyDataSetChanged();
            emptyState.setVisibility(View.VISIBLE);
            return;
        }

        filteredUserList.clear();
        userAdapter.notifyDataSetChanged(); // Clear the list immediately

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
                            if (userDetailsDoc.exists()) {
                                String username = userDetailsDoc.getString("username");
                                String profileImageUrl = userDetailsDoc.getString("profileImageUrl");

                                // Apply the search filter in the app (case-insensitive, with trimming)
                                if (username != null && username.trim().toLowerCase().startsWith(query.trim().toLowerCase())) {
                                    filteredUserList.add(new social_user(uid, username, profileImageUrl));
                                    userAdapter.notifyDataSetChanged();
                                }
                            }

                            if (remaining.decrementAndGet() == 0) {
                                emptyState.setVisibility(filteredUserList.isEmpty() ? View.VISIBLE : View.GONE);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (remaining.decrementAndGet() == 0) {
                                emptyState.setVisibility(filteredUserList.isEmpty() ? View.VISIBLE : View.GONE);
                            }
                        });
            }
        }).addOnFailureListener(e -> {
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText("Error loading users");
        });
    }
}