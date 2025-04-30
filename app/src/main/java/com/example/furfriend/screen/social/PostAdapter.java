package com.example.furfriend.screen.social;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.bumptech.glide.Glide;
import com.example.furfriend.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private static final String TAG = "PostAdapter";
    private List<Post> postList;
    private FirebaseFirestore db;
    private String currentUserId;

    public PostAdapter(List<Post> postList) {
        this.postList = postList;
        this.db = FirebaseFirestore.getInstance();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        Log.d(TAG, "PostAdapter initialized with list size: " + postList.size());
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder called");
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.social_item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        Log.d(TAG, "Binding post at position " + position + ": " + post.getPostId() +
                ", userName: " + post.getUserName() +
                ", contentText: " + post.getContentText() +
                ", dateTime: " + post.getDateTime() +
                ", mediaUrl: " + post.getMediaUrl());

        // Bind data to views
        holder.tvUserName.setText(post.getUserName() != null ? post.getUserName() : "Unknown");
        holder.tvContent.setText(post.getContentText() != null ? post.getContentText() : "");
        holder.tvTime.setText(post.getDateTime() != null ? post.getDateTime() : "Unknown Date");
        holder.tvLikeCount.setText(String.valueOf(post.getLikeCount()));
        holder.tvCommentCount.setText(String.valueOf(post.getCommentCount()));

        // Load avatar (placeholder for now, update with actual avatar if available)
        Glide.with(holder.itemView.getContext())
                .load(R.drawable.ic_social) // Replace with post.getAvatarUrl() if available
                .placeholder(R.drawable.ic_social)
                .error(R.drawable.ic_social)
                .into(holder.ivAvatar);

        // Update Like button state
        holder.btnLike.setImageResource(post.isLiked() ?
                R.drawable.ic_social_like2_icon : R.drawable.ic_social_like1_icon);
        holder.btnLike.setOnClickListener(v -> {
            if (post.isLiked()) {
                // Unlike the post
                db.collection("posts").document(post.getPostId())
                        .update("likes", FieldValue.arrayRemove(currentUserId))
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Unliked post: " + post.getPostId()))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to unlike post: " + post.getPostId(), e));
            } else {
                // Like the post
                db.collection("posts").document(post.getPostId())
                        .update("likes", FieldValue.arrayUnion(currentUserId))
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Liked post: " + post.getPostId()))
                        .addOnFailureListener(e -> Log.e(TAG, "Failed to like post: " + post.getPostId(), e));
            }
        });

        // Handle Comment button click
        holder.btnComment.setOnClickListener(v -> showCommentBottomSheet(holder.itemView.getContext(), post.getPostId()));

        // Load media if available
        if (post.getMediaUrl() != null && !post.getMediaUrl().isEmpty()) {
            Log.d(TAG, "Loading media for post " + post.getPostId() + ": " + post.getMediaUrl());
            Glide.with(holder.itemView.getContext())
                    .load(post.getMediaUrl())
                    .placeholder(R.drawable.ic_home)
                    .error(R.drawable.ic_home)
                    .into(holder.ivMedia);
            holder.ivMedia.setVisibility(View.VISIBLE);
        } else {
            Log.d(TAG, "No media for post " + post.getPostId());
            holder.ivMedia.setVisibility(View.GONE);
        }
    }

    private void showCommentBottomSheet(Context context, String postId) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.social_comment_sheet, null);
        bottomSheetDialog.setContentView(sheetView);

        // Initialize views
        RecyclerView recyclerComments = sheetView.findViewById(R.id.recycler_comments);
        EditText editComment = sheetView.findViewById(R.id.edit_comment);
        Button btnSendComment = sheetView.findViewById(R.id.btn_send_comment);

        // Set up RecyclerView for comments
        List<Comment> commentList = new ArrayList<>();
        CommentAdapter commentAdapter = new CommentAdapter(commentList);
        recyclerComments.setLayoutManager(new LinearLayoutManager(context));
        recyclerComments.setAdapter(commentAdapter);

        // Load comments from Firestore
        ListenerRegistration commentListener = db.collection("posts").document(postId)
                .collection("comments")
                .orderBy("timestamp")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error loading comments", error);
                        return;
                    }
                    if (querySnapshot == null) return;

                    commentList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String userId = doc.getString("userId");
                        String text = doc.getString("text");

                        // Fetch username for the comment
                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(userDoc -> {
                                    String userName = userDoc.exists() ? userDoc.getString("username") : "Unknown";
                                    Comment comment = new Comment(userName, text);
                                    commentList.add(comment);
                                    commentAdapter.notifyDataSetChanged();
                                    recyclerComments.scrollToPosition(commentList.size() - 1);
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Error fetching username for comment", e));
                    }
                });

        // Handle sending a new comment
        btnSendComment.setOnClickListener(v -> {
            String commentText = editComment.getText().toString().trim();
            if (!commentText.isEmpty() && currentUserId != null) {
                Map<String, Object> commentData = new HashMap<>();
                commentData.put("userId", currentUserId);
                commentData.put("text", commentText);
                commentData.put("timestamp", System.currentTimeMillis());

                db.collection("posts").document(postId)
                        .collection("comments")
                        .add(commentData)
                        .addOnSuccessListener(docRef -> {
                            // Update comment count in the post
                            db.collection("posts").document(postId)
                                    .update("commentCount", FieldValue.increment(1));
                            editComment.setText("");
                        })
                        .addOnFailureListener(e -> Log.e(TAG, "Error adding comment", e));
            }
        });

        // Show the bottom sheet
        bottomSheetDialog.show();

        // Clean up listener when bottom sheet is dismissed
        bottomSheetDialog.setOnDismissListener(dialog -> {
            if (commentListener != null) {
                commentListener.remove();
            }
        });
    }

    @Override
    public int getItemCount() {
        int size = postList.size();
        Log.d(TAG, "getItemCount: " + size);
        return size;
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar, ivMedia;
        TextView tvUserName, tvContent, tvTime, tvLikeCount, tvCommentCount;
        ImageButton btnLike, btnComment;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.social_post_avatar);
            ivMedia = itemView.findViewById(R.id.social_postImage);
            tvUserName = itemView.findViewById(R.id.social_post_userName);
            tvContent = itemView.findViewById(R.id.social_postText);
            tvTime = itemView.findViewById(R.id.social_postTime);
            tvLikeCount = itemView.findViewById(R.id.social_post_likeCount);
            tvCommentCount = itemView.findViewById(R.id.social_post_commentCount);
            btnLike = itemView.findViewById(R.id.social_post_btnLike);
            btnComment = itemView.findViewById(R.id.social_post_btnComment);
            Log.d(TAG, "PostViewHolder initialized, tvUserName: " + (tvUserName != null));
        }
    }
}