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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class social_SearchActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private List<social_user> filteredUserList;
    private EditText searchInput;
    private TextView emptyState;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.social_search);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

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
        emptyState.setVisibility(View.GONE);

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

        // Load all users initially
        searchUsers("");
    }

    private void searchUsers(String query) {
        filteredUserList.clear();
        Query firestoreQuery = db.collection("users");

        // If query is not empty, filter by username prefix
        if (!query.isEmpty()) {
            firestoreQuery = firestoreQuery
                    .whereGreaterThanOrEqualTo("username", query.toLowerCase())
                    .whereLessThanOrEqualTo("username", query.toLowerCase() + "\uf8ff");
        }

        firestoreQuery.get().addOnSuccessListener(querySnapshot -> {
            filteredUserList.clear();
            for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                String uid = document.getId();
                String username = document.getString("username");
                String profileImageUrl = document.getString("profileImageUrl");
                filteredUserList.add(new social_user(uid, username, profileImageUrl));
            }
            userAdapter.notifyDataSetChanged();
            emptyState.setVisibility(filteredUserList.isEmpty() ? View.VISIBLE : View.GONE);
        }).addOnFailureListener(e -> {
            emptyState.setVisibility(View.VISIBLE);
            emptyState.setText("Error loading users");
        });
    }
}