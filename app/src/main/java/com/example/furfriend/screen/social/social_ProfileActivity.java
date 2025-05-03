package com.example.furfriend.screen.social;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.furfriend.BaseActivity;
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

public class social_ProfileActivity extends BaseActivity {

    private static final String TAG = "ProfileActivity";
    private ImageView ivAvatar;
    private TextView tvFollowersCount, tvFollowingCount, tvUsername, tvPersonalizedText;
    private Button btnFollow;
    private TextView tvTabMyPosts, tvTabLikes;
    private RecyclerView recyclerProfileContent;
    private PostAdapter postAdapter;
    private UserAdapter userAdapter;
    private List<Post> contentList;
    private List<social_user> userList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private String profileUserId;
    private boolean isViewingOwnProfile;
    private ListenerRegistration postsListener;
    private ListenerRegistration likesListener;
    private ListenerRegistration usersListener;
    private boolean isFollowing;
    private enum DisplayMode { POSTS_LIKES, FOLLOWERS_FOLLOWING }
    private DisplayMode currentMode = DisplayMode.POSTS_LIKES;

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

        // Get profile user ID from Intent
        profileUserId = getIntent().getStringExtra("userId");
        Log.d(TAG, "Loading profile for userId: " + profileUserId);
        if (profileUserId == null || profileUserId.equals(currentUserId)) {
            profileUserId = currentUserId;
            isViewingOwnProfile = true;
            Log.d(TAG, "Defaulting to current user profile");
        } else {
            isViewingOwnProfile = false;
            Log.d(TAG, "Viewing other user profile");
        }

        // Initialize Views
        ivAvatar = findViewById(R.id.social_profile_avatar);
        tvFollowersCount = findViewById(R.id.social_profile_followers);
        tvFollowingCount = findViewById(R.id.social_profile_following);
        tvUsername = findViewById(R.id.social_profile_username);
        tvPersonalizedText = findViewById(R.id.social_profile_personalized_text);
        btnFollow = findViewById(R.id.btn_social_profile_follow);
        tvTabMyPosts = findViewById(R.id.social_profile_my_posts);
        tvTabLikes = findViewById(R.id.social_profile_likes);
        recyclerProfileContent = findViewById(R.id.recycler_social_profile_content);
        LinearLayout followersLayout = findViewById(R.id.social_profile_followers_layout);
        LinearLayout followingLayout = findViewById(R.id.social_profile_following_layout);

        // Set initial tab labels
        updateTabLabels();
        tvTabMyPosts.setSelected(true);

        // Initialize Adapters
        contentList = new ArrayList<>();
        postAdapter = new PostAdapter(contentList);
        userList = new ArrayList<>();
        userAdapter = new UserAdapter(userList, user -> {
            Intent intent = new Intent(this, social_ProfileActivity.class);
            intent.putExtra("userId", user.getUid());
            startActivity(intent);
        });
        recyclerProfileContent.setLayoutManager(new LinearLayoutManager(this));
        recyclerProfileContent.setAdapter(postAdapter); // Default to posts

        // Follow Button
        btnFollow.setVisibility(isViewingOwnProfile ? View.GONE : View.VISIBLE);
        btnFollow.setOnClickListener(v -> followUser());

        // Followers and Following Click Listeners
        followersLayout.setOnClickListener(v -> {
            currentMode = DisplayMode.FOLLOWERS_FOLLOWING;
            updateTabLabels();
            tvPersonalizedText.setVisibility(View.GONE);
            tvTabMyPosts.setSelected(true);
            tvTabLikes.setSelected(false);
            stopListeningToPosts();
            stopListeningToLikes();
            loadFollowers();
        });

        followingLayout.setOnClickListener(v -> {
            currentMode = DisplayMode.FOLLOWERS_FOLLOWING;
            updateTabLabels();
            tvPersonalizedText.setVisibility(View.GONE);
            tvTabMyPosts.setSelected(false);
            tvTabLikes.setSelected(true);
            stopListeningToPosts();
            stopListeningToLikes();
            loadFollowing();
        });

        tvUsername.setOnClickListener(v -> {
            currentMode = DisplayMode.POSTS_LIKES;
            updateTabLabels();
            tvPersonalizedText.setVisibility(View.VISIBLE);
            tvTabMyPosts.setSelected(true);
            tvTabLikes.setSelected(false);
            stopListeningToUsers();
            loadPosts();
        });

        // Tabs
        tvTabMyPosts.setOnClickListener(v -> {
            tvTabMyPosts.setSelected(true);
            tvTabLikes.setSelected(false);
            stopListeningToUsers();
            if (currentMode == DisplayMode.POSTS_LIKES) {
                stopListeningToLikes();
                loadPosts();
            } else {
                loadFollowers();
            }
        });

        tvTabLikes.setOnClickListener(v -> {
            tvTabMyPosts.setSelected(false);
            tvTabLikes.setSelected(true);
            stopListeningToUsers();
            if (currentMode == DisplayMode.POSTS_LIKES) {
                stopListeningToPosts();
                loadLikes();
            } else {
                loadFollowing();
            }
        });

        // Load user data and default tab (Posts)
        checkFollowStatus();
        loadUserData();
        loadPosts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentMode == DisplayMode.POSTS_LIKES) {
            if (tvTabMyPosts.isSelected()) {
                loadPosts();
            } else if (tvTabLikes.isSelected()) {
                loadLikes();
            }
        } else {
            if (tvTabMyPosts.isSelected()) {
                loadFollowers();
            } else if (tvTabLikes.isSelected()) {
                loadFollowing();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopListeningToPosts();
        stopListeningToLikes();
        stopListeningToUsers();
    }

    private void updateTabLabels() {
        if (currentMode == DisplayMode.POSTS_LIKES) {
            tvTabMyPosts.setText(isViewingOwnProfile ? R.string.social_profile_my_posts : R.string.social_profile_posts);
            tvTabLikes.setText(R.string.social_profile_likes);
        } else {
            tvTabMyPosts.setText(R.string.social_profile_followers);
            tvTabLikes.setText(R.string.social_profile_following);
        }
    }

    private void loadUserData() {
        Log.d(TAG, "Loading user data for: " + profileUserId);
        db.collection("users").document(profileUserId)
                .collection("profile").document("userDetails").get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String username = document.getString("username");
                        String avatarBase64 = document.getString("profilePictureBase64");
                        String personalizedText = document.getString("personalizedText");

                        tvUsername.setText(username != null ? username : "Unknown");
                        tvPersonalizedText.setText(personalizedText != null ? personalizedText : "");

                        // Load avatar using Glide with circular transformation
                        if (avatarBase64 != null && !avatarBase64.isEmpty()) {
                            String avatarDataUri = "data:image/jpeg;base64," + avatarBase64;
                            Glide.with(this)
                                    .load(avatarDataUri)
                                    .apply(RequestOptions.circleCropTransform())
                                    .placeholder(R.drawable.ic_social)
                                    .error(R.drawable.ic_social)
                                    .into(ivAvatar);
                        } else {
                            ivAvatar.setImageResource(R.drawable.ic_social); // Default avatar
                        }
                    } else {
                        Log.w(TAG, "User details not found for: " + profileUserId);
                        tvUsername.setText("User not found");
                        ivAvatar.setImageResource(R.drawable.ic_social); // Default avatar
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data", e);
                    tvUsername.setText("Error");
                    ivAvatar.setImageResource(R.drawable.ic_social); // Default avatar
                });

        // Load followers and following counts
        loadFollowersCount();
        loadFollowingCount();
    }

    private void loadFollowersCount() {
        db.collection("users").document(profileUserId).collection("followers").get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    tvFollowersCount.setText(formatCount(count));
                    db.collection("users").document(profileUserId)
                            .collection("profile").document("userDetails")
                            .update("followersCount", (long) count);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading followers count", e);
                    tvFollowersCount.setText("0");
                });
    }

    private void loadFollowingCount() {
        db.collection("users").document(profileUserId).collection("following").get()
                .addOnSuccessListener(querySnapshot -> {
                    int count = querySnapshot.size();
                    tvFollowingCount.setText(formatCount(count));
                    db.collection("users").document(profileUserId)
                            .collection("profile").document("userDetails")
                            .update("followingCount", (long) count);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading following count", e);
                    tvFollowingCount.setText("0");
                });
    }

    private void loadPosts() {
        stopListeningToPosts();
        recyclerProfileContent.setAdapter(postAdapter);
        contentList.clear();
        postAdapter.notifyDataSetChanged();

        postsListener = db.collection("posts")
                .whereEqualTo("userId", profileUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading posts", error);
                        return;
                    }
                    if (querySnapshot == null) return;

                    Set<String> userIds = new HashSet<>();
                    List<DocumentSnapshot> addedDocs = new ArrayList<>();
                    List<DocumentSnapshot> modifiedDocs = new ArrayList<>();

                    for (DocumentChange dc : querySnapshot.getDocumentChanges()) {
                        DocumentSnapshot document = dc.getDocument();
                        String userId = document.getString("userId");
                        if (userId != null) userIds.add(userId);

                        switch (dc.getType()) {
                            case ADDED: addedDocs.add(document); break;
                            case MODIFIED: modifiedDocs.add(document); break;
                            case REMOVED:
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

                    fetchUserNames(userIds, userNames -> {
                        for (DocumentSnapshot document : addedDocs) {
                            String postId = document.getId();
                            String userId = document.getString("userId");
                            String contentText = document.getString("content");
                            String mediaUrl = document.getString("mediaUrl");
                            Long timestamp = document.getLong("timestamp");
                            List<String> likes = (List<String>) document.get("likes");
                            Long likeCount = document.getLong("likeCount");
                            Long commentCount = document.getLong("commentCount");

                            if (userId == null) continue;

                            String userName = userNames.getOrDefault(userId, "Unknown");
                            Post post = new Post(postId, userId, userName, contentText, timestamp != null ? timestamp : 0, likes);
                            post.setMediaUrl(mediaUrl != null ? mediaUrl : "");
                            post.setCommentCount(commentCount != null ? commentCount.intValue() : 0);
                            post.setLikeCount(likeCount != null ? likeCount.intValue() : (likes != null ? likes.size() : 0));
                            post.setLiked(likes != null && likes.contains(currentUserId));

                            // Fetch avatar URL
                            db.collection("users").document(userId).collection("profile").document("userDetails").get()
                                    .addOnSuccessListener(userDoc -> {
                                        String avatarBase64 = userDoc.getString("profilePictureBase64");
                                        if (avatarBase64 != null) {
                                            post.setAvatarUrl(avatarBase64);
                                            postAdapter.notifyDataSetChanged();
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e(TAG, "Error fetching avatar for user: " + userId, e));

                            int insertPosition = 0;
                            for (int i = 0; i < contentList.size(); i++) {
                                if (contentList.get(i).getTimestamp() < post.getTimestamp()) break;
                                insertPosition++;
                            }
                            contentList.add(insertPosition, post);
                            postAdapter.notifyItemInserted(insertPosition);
                        }

                        for (DocumentSnapshot document : modifiedDocs) {
                            String postId = document.getId();
                            String userId = document.getString("userId");
                            String contentText = document.getString("content");
                            String mediaUrl = document.getString("mediaUrl");
                            Long timestamp = document.getLong("timestamp");
                            List<String> likes = (List<String>) document.get("likes");
                            Long likeCount = document.getLong("likeCount");
                            Long commentCount = document.getLong("commentCount");

                            if (userId == null) continue;

                            String userName = userNames.getOrDefault(userId, "Unknown");
                            for (int i = 0; i < contentList.size(); i++) {
                                Post post = contentList.get(i);
                                if (post.getPostId().equals(postId)) {
                                    post.setContentText(contentText);
                                    post.setMediaUrl(mediaUrl != null ? mediaUrl : "");
                                    post.setTimestamp(timestamp != null ? timestamp : 0);
                                    post.setLikeCount(likeCount != null ? likeCount.intValue() : (likes != null ? likes.size() : 0));
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
        stopListeningToLikes();
        recyclerProfileContent.setAdapter(postAdapter);
        contentList.clear();
        postAdapter.notifyDataSetChanged();

        likesListener = db.collection("posts")
                .whereArrayContains("likes", profileUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading likes", error);
                        return;
                    }
                    if (querySnapshot == null) return;

                    Set<String> userIds = new HashSet<>();
                    List<DocumentSnapshot> addedDocs = new ArrayList<>();
                    List<DocumentSnapshot> modifiedDocs = new ArrayList<>();

                    for (DocumentChange dc : querySnapshot.getDocumentChanges()) {
                        DocumentSnapshot document = dc.getDocument();
                        String userId = document.getString("userId");
                        if (userId != null) userIds.add(userId);

                        switch (dc.getType()) {
                            case ADDED: addedDocs.add(document); break;
                            case MODIFIED: modifiedDocs.add(document); break;
                            case REMOVED:
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

                    fetchUserNames(userIds, userNames -> {
                        for (DocumentSnapshot document : addedDocs) {
                            String postId = document.getId();
                            String userId = document.getString("userId");
                            String contentText = document.getString("content");
                            String mediaUrl = document.getString("mediaUrl");
                            Long timestamp = document.getLong("timestamp");
                            List<String> likes = (List<String>) document.get("likes");
                            Long likeCount = document.getLong("likeCount");
                            Long commentCount = document.getLong("commentCount");

                            if (userId == null) continue;

                            String userName = userNames.getOrDefault(userId, "Unknown");
                            Post post = new Post(postId, userId, userName, contentText, timestamp != null ? timestamp : 0, likes);
                            post.setMediaUrl(mediaUrl != null ? mediaUrl : "");
                            post.setCommentCount(commentCount != null ? commentCount.intValue() : 0);
                            post.setLikeCount(likeCount != null ? likeCount.intValue() : (likes != null ? likes.size() : 0));
                            post.setLiked(likes != null && likes.contains(currentUserId));

                            // Fetch avatar URL
                            db.collection("users").document(userId).collection("profile").document("userDetails").get()
                                    .addOnSuccessListener(userDoc -> {
                                        String avatarBase64 = userDoc.getString("profilePictureBase64");
                                        if (avatarBase64 != null) {
                                            post.setAvatarUrl(avatarBase64);
                                            postAdapter.notifyDataSetChanged();
                                        }
                                    })
                                    .addOnFailureListener(e -> Log.e(TAG, "Error fetching avatar for user: " + userId, e));

                            int insertPosition = 0;
                            for (int i = 0; i < contentList.size(); i++) {
                                if (contentList.get(i).getTimestamp() < post.getTimestamp()) break;
                                insertPosition++;
                            }
                            contentList.add(insertPosition, post);
                            postAdapter.notifyItemInserted(insertPosition);
                        }

                        for (DocumentSnapshot document : modifiedDocs) {
                            String postId = document.getId();
                            String userId = document.getString("userId");
                            String contentText = document.getString("content");
                            String mediaUrl = document.getString("mediaUrl");
                            Long timestamp = document.getLong("timestamp");
                            List<String> likes = (List<String>) document.get("likes");
                            Long likeCount = document.getLong("likeCount");
                            Long commentCount = document.getLong("commentCount");

                            if (userId == null) continue;

                            String userName = userNames.getOrDefault(userId, "Unknown");
                            for (int i = 0; i < contentList.size(); i++) {
                                Post post = contentList.get(i);
                                if (post.getPostId().equals(postId)) {
                                    post.setContentText(contentText);
                                    post.setMediaUrl(mediaUrl != null ? mediaUrl : "");
                                    post.setTimestamp(timestamp != null ? timestamp : 0);
                                    post.setLikeCount(likeCount != null ? likeCount.intValue() : (likes != null ? likes.size() : 0));
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

    private void loadFollowers() {
        stopListeningToUsers();
        recyclerProfileContent.setAdapter(userAdapter);
        userList.clear();
        userAdapter.notifyDataSetChanged();

        usersListener = db.collection("users").document(profileUserId).collection("followers")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading followers", error);
                        return;
                    }
                    if (querySnapshot == null) return;

                    List<String> followerIds = new ArrayList<>();
                    for (DocumentChange dc : querySnapshot.getDocumentChanges()) {
                        String followerId = dc.getDocument().getId();
                        switch (dc.getType()) {
                            case ADDED:
                                followerIds.add(followerId);
                                break;
                            case REMOVED:
                                for (int i = 0; i < userList.size(); i++) {
                                    if (userList.get(i).getUid().equals(followerId)) {
                                        userList.remove(i);
                                        userAdapter.notifyItemRemoved(i);
                                        break;
                                    }
                                }
                                break;
                        }
                    }

                    if (followerIds.isEmpty()) {
                        userAdapter.notifyDataSetChanged();
                        return;
                    }

                    for (String uid : followerIds) {
                        db.collection("users").document(uid).collection("profile").document("userDetails").get()
                                .addOnSuccessListener(document -> {
                                    if (document.exists()) {
                                        String username = document.getString("username");
                                        String avatarBase64 = document.getString("profilePictureBase64");
                                        social_user user = new social_user(uid, username, avatarBase64);
                                        userList.add(user);
                                        userAdapter.notifyItemInserted(userList.size() - 1);
                                    }
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error fetching user details for: " + uid, e));
                    }
                });
    }

    private void loadFollowing() {
        stopListeningToUsers();
        recyclerProfileContent.setAdapter(userAdapter);
        userList.clear();
        userAdapter.notifyDataSetChanged();

        usersListener = db.collection("users").document(profileUserId).collection("following")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading following", error);
                        return;
                    }
                    if (querySnapshot == null) return;

                    List<String> followingIds = new ArrayList<>();
                    for (DocumentChange dc : querySnapshot.getDocumentChanges()) {
                        String followingId = dc.getDocument().getId();
                        switch (dc.getType()) {
                            case ADDED:
                                followingIds.add(followingId);
                                break;
                            case REMOVED:
                                for (int i = 0; i < userList.size(); i++) {
                                    if (userList.get(i).getUid().equals(followingId)) {
                                        userList.remove(i);
                                        userAdapter.notifyItemRemoved(i);
                                        break;
                                    }
                                }
                                break;
                        }
                    }

                    if (followingIds.isEmpty()) {
                        userAdapter.notifyDataSetChanged();
                        return;
                    }

                    for (String uid : followingIds) {
                        db.collection("users").document(uid).collection("profile").document("userDetails").get()
                                .addOnSuccessListener(document -> {
                                    if (document.exists()) {
                                        String username = document.getString("username");
                                        String avatarBase64 = document.getString("profilePictureBase64");
                                        social_user user = new social_user(uid, username, avatarBase64);
                                        userList.add(user);
                                        userAdapter.notifyItemInserted(userList.size() - 1);
                                    }
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error fetching user details for: " + uid, e));
                    }
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

    private void stopListeningToUsers() {
        if (usersListener != null) {
            usersListener.remove();
            usersListener = null;
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

    private void checkFollowStatus() {
        if (isViewingOwnProfile) return;

        db.collection("users").document(profileUserId)
                .collection("followers").document(currentUserId).get()
                .addOnSuccessListener(document -> {
                    isFollowing = document.exists();
                    btnFollow.setText(isFollowing ? "Unfollow" : "Follow");
                    btnFollow.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking follow status", e);
                    btnFollow.setText("Follow");
                    btnFollow.setEnabled(true);
                });
    }

    private void followUser() {
        if (isFollowing) {
            // Unfollow
            db.collection("users").document(profileUserId)
                    .collection("followers").document(currentUserId).delete()
                    .addOnSuccessListener(aVoid -> {
                        db.collection("users").document(currentUserId)
                                .collection("following").document(profileUserId).delete()
                                .addOnSuccessListener(aVoid2 -> {
                                    isFollowing = false;
                                    btnFollow.setText("Follow");
                                    loadFollowersCount();
                                    loadFollowingCount();
                                });
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error unfollowing user", e));
        } else {
            // Follow
            Map<String, Object> data = new HashMap<>();
            data.put("timestamp", System.currentTimeMillis());

            db.collection("users").document(profileUserId)
                    .collection("followers").document(currentUserId).set(data)
                    .addOnSuccessListener(aVoid -> {
                        db.collection("users").document(currentUserId)
                                .collection("following").document(profileUserId).set(data)
                                .addOnSuccessListener(aVoid2 -> {
                                    isFollowing = true;
                                    btnFollow.setText("Unfollow");
                                    loadFollowersCount();
                                    loadFollowingCount();

                                    // Create a notification for the followed user
                                    createFollowNotification();
                                });
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "Error following user", e));
        }
    }

    private void createFollowNotification() {
        db.collection("users").document(currentUserId).collection("profile").document("userDetails").get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String username = document.getString("username");
                        String avatarBase64 = document.getString("profilePictureBase64");

                        Map<String, Object> notificationData = new HashMap<>();
                        notificationData.put("recipientId", profileUserId);
                        notificationData.put("userId", currentUserId);
                        notificationData.put("username", username);
                        notificationData.put("type", "follow");
                        notificationData.put("postId", null);
                        notificationData.put("content", username + " followed you");
                        notificationData.put("timestamp", System.currentTimeMillis());
                        notificationData.put("avatarUrl", avatarBase64 != null ? "data:image/jpeg;base64," + avatarBase64 : "");

                        db.collection("notifications").add(notificationData)
                                .addOnSuccessListener(docRef -> Log.d(TAG, "Follow notification created for user: " + profileUserId))
                                .addOnFailureListener(e -> Log.e(TAG, "Error creating follow notification", e));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching follower details", e));
    }

    private interface OnUserNamesFetchedListener {
        void onFetched(Map<String, String> userNames);
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