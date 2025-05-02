package com.example.furfriend.screen.social;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
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

    private List<Post> postList;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String currentUserId;

    public PostAdapter(List<Post> postList) {
        this.postList = postList;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    @Override
    public PostViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.social_item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(PostViewHolder holder, int position) {
        Post post = postList.get(position);

        // Bind data to views
        holder.tvUserName.setText(post.getUserName() != null ? post.getUserName() : "Unknown");
        holder.tvContent.setText(post.getContentText() != null ? post.getContentText() : "");
        holder.tvTime.setText(post.getDateTime() != null ? post.getDateTime() : "Unknown Date");
        holder.tvLikeCount.setText(String.valueOf(post.getLikeCount()));
        holder.tvCommentCount.setText(String.valueOf(post.getCommentCount()));

        // Load avatar (already Base64, using data URI)
        String avatarDataUri = post.getAvatarUrl() != null ? "data:image/jpeg;base64," + post.getAvatarUrl() : null;
        if (avatarDataUri != null) {
            try {
                byte[] decodedAvatar = Base64.decode(post.getAvatarUrl(), Base64.DEFAULT);
                Bitmap avatarBitmap = BitmapFactory.decodeByteArray(decodedAvatar, 0, decodedAvatar.length);
                holder.ivAvatar.setImageBitmap(avatarBitmap);
            } catch (Exception e) {
                Log.e("PostAdapter", "Error decoding avatar Base64 for post " + post.getPostId(), e);
                holder.ivAvatar.setImageResource(R.drawable.ic_social);
            }
        } else {
            holder.ivAvatar.setImageResource(R.drawable.ic_social);
        }

        // Update Like button state
        holder.btnLike.setImageResource(post.isLiked() ?
                R.drawable.ic_social_like2_icon : R.drawable.ic_social_like1_icon);
        holder.btnLike.setOnClickListener(v -> {
            Map<String, Object> updates = new HashMap<>();
            if (post.isLiked()) {
                // Unlike the post
                updates.put("likes", FieldValue.arrayRemove(currentUserId));
                updates.put("likeCount", FieldValue.increment(-1));
                db.collection("posts").document(post.getPostId())
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            post.setLiked(false);
                            notifyItemChanged(position);
                        })
                        .addOnFailureListener(e -> Log.e("PostAdapter", "Error unliking post", e));
            } else {
                // Like the post
                updates.put("likes", FieldValue.arrayUnion(currentUserId));
                updates.put("likeCount", FieldValue.increment(1));
                db.collection("posts").document(post.getPostId())
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            post.setLiked(true);
                            notifyItemChanged(position);

                            // Create a notification for the post owner (if not liking own post)
                            if (!post.getUserId().equals(currentUserId)) {
                                createLikeNotification(post);
                            }
                        })
                        .addOnFailureListener(e -> Log.e("PostAdapter", "Error liking post", e));
            }
        });

        // Handle Comment button click
        holder.btnComment.setOnClickListener(v -> showCommentBottomSheet(holder.itemView.getContext(), post.getPostId(), post.getUserId()));

        // Load media (now Base64 instead of URL)
        if (post.getMediaUrl() != null && !post.getMediaUrl().isEmpty()) {
            try {
                byte[] decodedMedia = Base64.decode(post.getMediaUrl(), Base64.DEFAULT);
                Bitmap mediaBitmap = BitmapFactory.decodeByteArray(decodedMedia, 0, decodedMedia.length);
                holder.ivMedia.setImageBitmap(mediaBitmap);
                holder.ivMedia.setVisibility(View.VISIBLE);
            } catch (Exception e) {
                Log.e("PostAdapter", "Error decoding media Base64 for post " + post.getPostId(), e);
                holder.ivMedia.setVisibility(View.GONE);
            }
        } else {
            holder.ivMedia.setVisibility(View.GONE);
        }
    }

    private void showCommentBottomSheet(Context context, String postId, String postUserId) {
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
                        return;
                    }
                    if (querySnapshot == null) return;

                    commentList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String userId = doc.getString("userId");
                        String text = doc.getString("text");

                        // Fetch username for the comment
                        db.collection("users").document(userId)
                                .collection("profile").document("userDetails").get()
                                .addOnSuccessListener(userDoc -> {
                                    String userName = userDoc.exists() ? userDoc.getString("username") : "Unknown";
                                    Comment comment = new Comment(userName, text);
                                    commentList.add(comment);
                                    commentAdapter.notifyDataSetChanged();
                                    recyclerComments.scrollToPosition(commentList.size() - 1);
                                })
                                .addOnFailureListener(e -> {});
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
                                    .update("commentCount", FieldValue.increment(1))
                                    .addOnSuccessListener(aVoid -> {
                                        editComment.setText("");
                                        // Create a notification for the post owner (if not commenting on own post)
                                        if (!postUserId.equals(currentUserId)) {
                                            createCommentNotification(postId, commentText, postUserId);
                                        }
                                    })
                                    .addOnFailureListener(e -> {});
                        })
                        .addOnFailureListener(e -> {});
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

    private void createLikeNotification(Post post) {
        db.collection("users").document(currentUserId).collection("profile").document("userDetails").get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String username = document.getString("username");
                        String avatarUrl = document.getString("avatarUrl");

                        Map<String, Object> notificationData = new HashMap<>();
                        notificationData.put("recipientId", post.getUserId());
                        notificationData.put("userId", currentUserId);
                        notificationData.put("username", username);
                        notificationData.put("type", "like");
                        notificationData.put("postId", post.getPostId());
                        notificationData.put("content", username + " liked your post");
                        notificationData.put("timestamp", System.currentTimeMillis());
                        notificationData.put("avatarUrl", avatarUrl != null ? avatarUrl : "");

                        db.collection("notifications").add(notificationData)
                                .addOnSuccessListener(docRef -> Log.d("PostAdapter", "Like notification created"))
                                .addOnFailureListener(e -> Log.e("PostAdapter", "Error creating like notification", e));
                    }
                })
                .addOnFailureListener(e -> Log.e("PostAdapter", "Error fetching liker details", e));
    }

    private void createCommentNotification(String postId, String commentText, String postUserId) {
        db.collection("users").document(currentUserId).collection("profile").document("userDetails").get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String username = document.getString("username");
                        String avatarUrl = document.getString("avatarUrl");

                        Map<String, Object> notificationData = new HashMap<>();
                        notificationData.put("recipientId", postUserId);
                        notificationData.put("userId", currentUserId);
                        notificationData.put("username", username);
                        notificationData.put("type", "comment");
                        notificationData.put("postId", postId);
                        notificationData.put("content", username + " commented on your post: " + commentText);
                        notificationData.put("timestamp", System.currentTimeMillis());
                        notificationData.put("avatarUrl", avatarUrl != null ? avatarUrl : "");

                        db.collection("notifications").add(notificationData)
                                .addOnSuccessListener(docRef -> Log.d("PostAdapter", "Comment notification created"))
                                .addOnFailureListener(e -> Log.e("PostAdapter", "Error creating comment notification", e));
                    }
                })
                .addOnFailureListener(e -> Log.e("PostAdapter", "Error fetching commenter details", e));
    }

    @Override
    public int getItemCount() {
        return postList.size();
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
        }
    }
}