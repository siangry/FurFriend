package com.example.furfriend.screen.social;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.furfriend.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class social_ProfileActivity extends AppCompatActivity {

    private ImageView ivAvatar;
    private TextView tvFollowersCount, tvFollowingCount, tvUsername, tvPersonalizedText;
    private Button btnFollow;
    private TextView tvTabMyPosts, tvTabLikes;
    private RecyclerView recyclerProfileContent;
    private PostAdapter postAdapter;
    private List<Post> contentList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private String profileUserId;
    private boolean isViewingOwnProfile;
    private ListenerRegistration postsListener;
    private ListenerRegistration likesListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.social_profile);

        // Initialize Firebase
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
            Intent intent = new Intent(this, social_NotificationActivity.class);
            startActivity(intent);
            finish();
        });

        // Get profile user ID from Intent (if viewing another user's profile)
        profileUserId = getIntent().getStringExtra("userId");
        if (profileUserId == null || profileUserId.equals(currentUserId)) {
            profileUserId = currentUserId;
            isViewingOwnProfile = true;
        } else {
            isViewingOwnProfile = false;
        }

        // Initialize Views
        ivAvatar = findViewById(R.id.social_profile_avatar);
        tvFollowersCount = findViewById(R.id.social_profile_followers);
        tvFollowingCount = findViewById(R.id.social_profile_following);
        tvUsername = findViewById(R.id.social_profile_username);
        tvPersonalizedText = findViewById(R.id.social_profile_personalized_text);
        btnFollow = findViewById(R.id.btn_social_profile_follow);
        tvTabMyPosts = findViewById(R.id.social_profile_my_posts);
        tvTabMyPosts.setSelected(true);
        tvTabLikes = findViewById(R.id.social_profile_likes);
        recyclerProfileContent = findViewById(R.id.recycler_social_profile_content);

        // Set up RecyclerView
        contentList = new ArrayList<>();
        postAdapter = new PostAdapter(contentList);
        recyclerProfileContent.setLayoutManager(new LinearLayoutManager(this));
        recyclerProfileContent.setAdapter(postAdapter);

        // Follow Button
        btnFollow.setVisibility(isViewingOwnProfile ? View.GONE : View.VISIBLE);
        btnFollow.setOnClickListener(v -> followUser());

        // Tabs
        tvTabMyPosts.setOnClickListener(v -> {
            tvTabMyPosts.setSelected(true);
            tvTabLikes.setSelected(false);
            stopListeningToLikes();
            loadPosts();
        });
        tvTabLikes.setOnClickListener(v -> {
            tvTabMyPosts.setSelected(false);
            tvTabLikes.setSelected(true);
            stopListeningToPosts();
            loadLikes();
        });

        // Load user data and default tab (My Posts)
        loadUserData();
        loadPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the current tab
        if (tvTabMyPosts.isSelected()) {
            loadPosts();
        } else if (tvTabLikes.isSelected()) {
            loadLikes();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up listeners to avoid memory leaks
        stopListeningToPosts();
        stopListeningToLikes();
    }

    private void loadUserData() {
        db.collection("users").document(profileUserId)
                .collection("profile").document("userDetails").get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String username = document.getString("username");
                        String avatarUrl = document.getString("avatarUrl");
                        String personalizedText = document.getString("personalizedText");
                        Long followersCount = document.getLong("followersCount");
                        Long followingCount = document.getLong("followingCount");

                        tvUsername.setText(username != null ? username : "Unknown");
                        tvPersonalizedText.setText(personalizedText != null ? personalizedText : "");
                        tvFollowersCount.setText(followersCount != null ? formatCount(followersCount) : "0");
                        tvFollowingCount.setText(followingCount != null ? String.valueOf(followingCount) : "0");

                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(avatarUrl)
                                    .placeholder(R.drawable.ic_social)
                                    .error(R.drawable.ic_social)
                                    .into(ivAvatar);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    tvUsername.setText("Error");
                });
    }

    private void loadPosts() {
        stopListeningToPosts(); // Remove any existing listener
        contentList.clear();
        postAdapter.notifyDataSetChanged();

        // Set up real-time listener
        postsListener = db.collection("posts")
                .whereEqualTo("userId", profileUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (querySnapshot == null) {
                        return;
                    }

                    // Process changes
                    Set<String> userIds = new HashSet<>();
                    List<DocumentSnapshot> addedDocs = new ArrayList<>();
                    List<DocumentSnapshot> modifiedDocs = new ArrayList<>();

                    for (DocumentChange dc : querySnapshot.getDocumentChanges()) {
                        DocumentSnapshot document = dc.getDocument();
                        String userId = document.getString("userId");
                        if (userId != null) {
                            userIds.add(userId);
                        }

                        switch (dc.getType()) {
                            case ADDED:
                                addedDocs.add(document);
                                break;
                            case MODIFIED:
                                // Update the existing post in contentList without changing its position
                                for (int i = 0; i < contentList.size(); i++) {
                                    if (contentList.get(i).getPostId().equals(document.getId())) {
                                        modifiedDocs.add(document);
                                        break;
                                    }
                                }
                                break;
                            case REMOVED:
                                // Remove the post from contentList
                                for (int i = 0; i < contentList.size(); i++) {
                                    if (contentList.get(i).getPostId().equals(document.getId())) {
                                        contentList.remove(i);
                                        postAdapter.notifyItemRemoved(i);
                                        break;
                                    }
                                }
                                break;
                        }
                    }

                    if (userIds.isEmpty()) {
                        postAdapter.notifyDataSetChanged();
                        return;
                    }

                    // Fetch usernames for all userIds
                    fetchUserNames(userIds, userNames -> {
                        // Handle ADDED posts
                        for (DocumentSnapshot document : addedDocs) {
                            String postId = document.getId();
                            String userId = document.getString("userId");
                            String contentText = document.getString("content");
                            String mediaUrl = document.getString("mediaUrl");
                            Long timestamp = document.getLong("timestamp");
                            List<String> likes = (List<String>) document.get("likes");
                            Long commentCount = document.getLong("commentCount");

                            if (userId == null) {
                                continue;
                            }

                            String userName = userNames.getOrDefault(userId, "Unknown");

                            Post post = new Post(
                                    postId,
                                    userId,
                                    userName,
                                    contentText,
                                    timestamp != null ? timestamp : 0,
                                    likes
                            );

                            post.setMediaUrl(mediaUrl != null ? mediaUrl : "");
                            post.setCommentCount(commentCount != null ? commentCount.intValue() : 0);

                            if (likes != null && likes.contains(currentUserId)) {
                                post.setLiked(true);
                            }

                            // Add post in the correct position (sorted by timestamp)
                            int insertPosition = 0;
                            for (int i = 0; i < contentList.size(); i++) {
                                if (contentList.get(i).getTimestamp() < post.getTimestamp()) {
                                    break;
                                }
                                insertPosition++;
                            }
                            contentList.add(insertPosition, post);
                            postAdapter.notifyItemInserted(insertPosition);
                        }

                        // Handle MODIFIED posts
                        for (DocumentSnapshot document : modifiedDocs) {
                            String postId = document.getId();
                            String userId = document.getString("userId");
                            String contentText = document.getString("content");
                            String mediaUrl = document.getString("mediaUrl");
                            Long timestamp = document.getLong("timestamp");
                            List<String> likes = (List<String>) document.get("likes");
                            Long commentCount = document.getLong("commentCount");

                            if (userId == null) {
                                continue;
                            }

                            String userName = userNames.getOrDefault(userId, "Unknown");

                            // Find the post and update it in place
                            for (int i = 0; i < contentList.size(); i++) {
                                Post post = contentList.get(i);
                                if (post.getPostId().equals(postId)) {
                                    post.contentText = contentText;
                                    post.setMediaUrl(mediaUrl != null ? mediaUrl : "");
                                    post.setTimestamp(timestamp != null ? timestamp : 0);
                                    post.setLikeCount(likes != null ? likes.size() : 0);
                                    post.setCommentCount(commentCount != null ? commentCount.intValue() : 0);
                                    post.setLiked(likes != null && likes.contains(currentUserId));
                                    postAdapter.notifyItemChanged(i);
                                    break;
                                }
                            }
                        }
                    });
                });
    }

    private void loadLikes() {
        stopListeningToLikes(); // Remove any existing listener
        contentList.clear();
        postAdapter.notifyDataSetChanged();

        // Set up real-time listener
        likesListener = db.collection("posts")
                .whereArrayContains("likes", profileUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (querySnapshot == null) {
                        return;
                    }

                    // Process changes
                    Set<String> userIds = new HashSet<>();
                    List<DocumentSnapshot> addedDocs = new ArrayList<>();
                    List<DocumentSnapshot> modifiedDocs = new ArrayList<>();

                    for (DocumentChange dc : querySnapshot.getDocumentChanges()) {
                        DocumentSnapshot document = dc.getDocument();
                        String userId = document.getString("userId");
                        if (userId != null) {
                            userIds.add(userId);
                        }

                        switch (dc.getType()) {
                            case ADDED:
                                addedDocs.add(document);
                                break;
                            case MODIFIED:
                                // Update the existing post in contentList without changing its position
                                for (int i = 0; i < contentList.size(); i++) {
                                    if (contentList.get(i).getPostId().equals(document.getId())) {
                                        modifiedDocs.add(document);
                                        break;
                                    }
                                }
                                break;
                            case REMOVED:
                                // Remove the post from contentList
                                for (int i = 0; i < contentList.size(); i++) {
                                    if (contentList.get(i).getPostId().equals(document.getId())) {
                                        contentList.remove(i);
                                        postAdapter.notifyItemRemoved(i);
                                        break;
                                    }
                                }
                                break;
                        }
                    }

                    if (userIds.isEmpty()) {
                        postAdapter.notifyDataSetChanged();
                        return;
                    }

                    // Fetch usernames for all userIds
                    fetchUserNames(userIds, userNames -> {
                        // Handle ADDED posts
                        for (DocumentSnapshot document : addedDocs) {
                            String postId = document.getId();
                            String userId = document.getString("userId");
                            String contentText = document.getString("content");
                            String mediaUrl = document.getString("mediaUrl");
                            Long timestamp = document.getLong("timestamp");
                            List<String> likes = (List<String>) document.get("likes");
                            Long commentCount = document.getLong("commentCount");

                            if (userId == null) {
                                continue;
                            }

                            String userName = userNames.getOrDefault(userId, "Unknown");

                            Post post = new Post(
                                    postId,
                                    userId,
                                    userName,
                                    contentText,
                                    timestamp != null ? timestamp : 0,
                                    likes
                            );

                            post.setMediaUrl(mediaUrl != null ? mediaUrl : "");
                            post.setCommentCount(commentCount != null ? commentCount.intValue() : 0);

                            if (likes != null && likes.contains(currentUserId)) {
                                post.setLiked(true);
                            }

                            // Add post in the correct position (sorted by timestamp)
                            int insertPosition = 0;
                            for (int i = 0; i < contentList.size(); i++) {
                                if (contentList.get(i).getTimestamp() < post.getTimestamp()) {
                                    break;
                                }
                                insertPosition++;
                            }
                            contentList.add(insertPosition, post);
                            postAdapter.notifyItemInserted(insertPosition);
                        }

                        // Handle MODIFIED posts
                        for (DocumentSnapshot document : modifiedDocs) {
                            String postId = document.getId();
                            String userId = document.getString("userId");
                            String contentText = document.getString("content");
                            String mediaUrl = document.getString("mediaUrl");
                            Long timestamp = document.getLong("timestamp");
                            List<String> likes = (List<String>) document.get("likes");
                            Long commentCount = document.getLong("commentCount");

                            if (userId == null) {
                                continue;
                            }

                            String userName = userNames.getOrDefault(userId, "Unknown");

                            // Find the post and update it in place
                            for (int i = 0; i < contentList.size(); i++) {
                                Post post = contentList.get(i);
                                if (post.getPostId().equals(postId)) {
                                    post.contentText = contentText;
                                    post.setMediaUrl(mediaUrl != null ? mediaUrl : "");
                                    post.setTimestamp(timestamp != null ? timestamp : 0);
                                    post.setLikeCount(likes != null ? likes.size() : 0);
                                    post.setCommentCount(commentCount != null ? commentCount.intValue() : 0);
                                    post.setLiked(likes != null && likes.contains(currentUserId));
                                    postAdapter.notifyItemChanged(i);
                                    break;
                                }
                            }
                        }
                    });
                });
    }

    private void stopListeningToPosts() {
        if (postsListener != null) {
            postsListener.remove();
            postsListener = null;
        }
    }

    private void stopListeningToLikes() {
        if (likesListener != null) {
            likesListener.remove();
            likesListener = null;
        }
    }

    private void fetchUserNames(Set<String> userIds, OnUserNamesFetchedListener listener) {
        if (userIds.isEmpty()) {
            listener.onFetched(new HashMap<>());
            return;
        }

        Map<String, String> userNames = new HashMap<>();
        AtomicInteger remaining = new AtomicInteger(userIds.size());

        for (String userId : userIds) {
            db.collection("users").document(userId)
                    .collection("profile").document("userDetails").get()
                    .addOnSuccessListener(document -> {
                        String userName = document.exists() ? document.getString("username") : null;
                        userNames.put(userId, userName != null ? userName : "Unknown");
                        if (remaining.decrementAndGet() == 0) {
                            listener.onFetched(userNames);
                        }
                    })
                    .addOnFailureListener(e -> {
                        userNames.put(userId, "Unknown");
                        if (remaining.decrementAndGet() == 0) {
                            listener.onFetched(userNames);
                        }
                    });
        }
    }

    private interface OnUserNamesFetchedListener {
        void onFetched(Map<String, String> userNames);
    }

    private void followUser() {
        // Update followers count and following list in Firestore
        // Simplified for now; implement follow logic as needed
        btnFollow.setText("Following");
        btnFollow.setEnabled(false);
    }

    private String formatCount(long count) {
        if (count >= 1_000_000) {
            return String.format("%.1fM", count / 1_000_000.0);
        } else if (count >= 1_000) {
            return String.format("%.1fK", count / 1_000.0);
        }
        return String.valueOf(count);
    }
}