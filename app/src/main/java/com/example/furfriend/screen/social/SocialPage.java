package com.example.furfriend.screen.social;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import com.example.furfriend.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class SocialPage extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter postAdapter;
    private List<Post> postList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;
    private ListenerRegistration postsListener;

    public SocialPage() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.screen_social_page, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            return view;
        }

        recyclerView = view.findViewById(R.id.recycler_posts);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Profile Button
        ImageButton btnProfile = view.findViewById(R.id.btn_social_profile);
        btnProfile.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), social_ProfileActivity.class);
            startActivity(intent);
        });

        // Search Button
        ImageButton btnSearch = view.findViewById(R.id.btn_social_search);
        btnSearch.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), social_SearchActivity.class);
            startActivity(intent);
        });

        // Notification Button
        ImageButton btnNotification = view.findViewById(R.id.btn_social_notification);
        btnNotification.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), social_NotificationActivity.class);
            startActivity(intent);
        });

        // Add Post Button
        AppCompatImageButton fabAddPost = view.findViewById(R.id.fab_social_add_post);
        fabAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddPostActivity.class);
            startActivity(intent);
        });

        postList = new ArrayList<>();
        postAdapter = new PostAdapter(postList);
        recyclerView.setAdapter(postAdapter);

        // Load posts from Firestore
        loadPosts();

        return view;
    }

    private void loadPosts() {
        // Clean up any existing listener
        stopListeningToPosts();

        postList.clear();
        postAdapter.notifyDataSetChanged();

        // Set up real-time listener
        postsListener = db.collection("posts")
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
                                // Update the existing post in postList without changing its position
                                for (int i = 0; i < postList.size(); i++) {
                                    if (postList.get(i).getPostId().equals(document.getId())) {
                                        modifiedDocs.add(document);
                                        break;
                                    }
                                }
                                break;
                            case REMOVED:
                                // Remove the post from postList
                                for (int i = 0; i < postList.size(); i++) {
                                    if (postList.get(i).getPostId().equals(document.getId())) {
                                        postList.remove(i);
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

                    // Fetch usernames and avatars for all userIds
                    fetchUserData(userIds, (userNames, avatarMap) -> {
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
                            String avatarBase64 = avatarMap.getOrDefault(userId, "");

                            // Format timestamp to date-time string
                            String dateTime = timestamp != null ?
                                    new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a").format(new Date(timestamp)) :
                                    "Unknown Date";

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
                            post.setDateTime(dateTime);
                            post.setAvatarUrl(avatarBase64);
                            if (likes != null && likes.contains(currentUserId)) {
                                post.setLiked(true);
                            }

                            // Add post in the correct position (sorted by timestamp)
                            int insertPosition = 0;
                            for (int i = 0; i < postList.size(); i++) {
                                if (postList.get(i).getTimestamp() < post.getTimestamp()) {
                                    break;
                                }
                                insertPosition++;
                            }
                            postList.add(insertPosition, post);
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
                            String avatarBase64 = avatarMap.getOrDefault(userId, "");

                            // Format timestamp to date-time string
                            String dateTime = timestamp != null ?
                                    new SimpleDateFormat("MMM dd, yyyy 'at' h:mm a").format(new Date(timestamp)) :
                                    "Unknown Date";

                            // Find the post and update it in place
                            for (int i = 0; i < postList.size(); i++) {
                                Post post = postList.get(i);
                                if (post.getPostId().equals(postId)) {
                                    post.setContentText(contentText);
                                    post.setMediaUrl(mediaUrl != null ? mediaUrl : "");
                                    post.setTimestamp(timestamp != null ? timestamp : 0);
                                    post.setLikeCount(likes != null ? likes.size() : 0);
                                    post.setCommentCount(commentCount != null ? commentCount.intValue() : 0);
                                    post.setDateTime(dateTime);
                                    post.setLiked(likes != null && likes.contains(currentUserId));
                                    post.setAvatarUrl(avatarBase64);
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

    private void fetchUserData(Set<String> userIds, OnUserDataFetchedListener listener) {
        if (userIds.isEmpty()) {
            listener.onFetched(new HashMap<>(), new HashMap<>());
            return;
        }

        Map<String, String> userNames = new HashMap<>();
        Map<String, String> avatarMap = new HashMap<>();
        AtomicInteger remaining = new AtomicInteger(userIds.size());

        for (String userId : userIds) {
            db.collection("users").document(userId)
                    .collection("profile").document("userDetails").get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String userName = document.getString("username");
                            String avatarBase64 = document.getString("profilePictureBase64");
                            userNames.put(userId, userName != null ? userName : "Unknown");
                            avatarMap.put(userId, avatarBase64 != null ? avatarBase64 : "");
                        } else {
                            userNames.put(userId, "Unknown");
                            avatarMap.put(userId, "");
                        }
                        if (remaining.decrementAndGet() == 0) {
                            listener.onFetched(userNames, avatarMap);
                        }
                    })
                    .addOnFailureListener(e -> {
                        userNames.put(userId, "Unknown");
                        avatarMap.put(userId, "");
                        if (remaining.decrementAndGet() == 0) {
                            listener.onFetched(userNames, avatarMap);
                        }
                    });
        }
    }

    private interface OnUserDataFetchedListener {
        void onFetched(Map<String, String> userNames, Map<String, String> avatarMap);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopListeningToPosts();
    }
}