package com.example.furfriend.screen.social;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.furfriend.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class AddPostActivity extends AppCompatActivity {

    private static final int REQUEST_GALLERY = 1001;
    private static final int REQUEST_CAMERA = 1002;

    private boolean isPublic = true;
    private EditText postContent;
    private ImageView mediaPreviewImage; // Image Preview
    private VideoView mediaPreviewVideo; // Video Preview
    private Uri selectedMediaUri = null;
    private FrameLayout mediaContainer;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.social_add_post);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        auth = FirebaseAuth.getInstance();

        ImageButton btnBack = findViewById(R.id.btn_social_return);
        ImageButton btnPost = findViewById(R.id.btn_social_post);
        ImageButton btnGalleryCamera = findViewById(R.id.btn_social_media);
        ImageButton btnPublic = findViewById(R.id.btn_social_public);
        ImageButton btnPrivate = findViewById(R.id.btn_social_private);
        postContent = findViewById(R.id.social_input_post);
        mediaPreviewImage = findViewById(R.id.social_image_preview); // For Image Preview
        mediaPreviewVideo = findViewById(R.id.social_video_preview); // For Video Preview
        mediaContainer = findViewById(R.id.social_media_preview_container);

        // Set default state for Public button
        btnPublic.setImageResource(R.drawable.ic_social_addpost_public2);  // Public selected by default
        btnPrivate.setImageResource(R.drawable.ic_social_addpost_private1);  // Private deselected

        // Back
        btnBack.setOnClickListener(v -> finish());

        // Visibility toggle
        btnPublic.setOnClickListener(v -> {
            isPublic = true;
            btnPublic.setImageResource(R.drawable.ic_social_addpost_public2);
            btnPrivate.setImageResource(R.drawable.ic_social_addpost_private1);
        });

        btnPrivate.setOnClickListener(v -> {
            isPublic = false;
            btnPublic.setImageResource(R.drawable.ic_social_addpost_public1);
            btnPrivate.setImageResource(R.drawable.ic_social_addpost_private2);
        });

        // Media select popup
        btnGalleryCamera.setOnClickListener(v -> showMediaPickerDialog());

        // Post button
        btnPost.setOnClickListener(v -> {
            String content = postContent.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "Please write something...", Toast.LENGTH_SHORT).show();
                return;
            }

            // Save post to Firestore
            savePostToFirestore(content);
            finish(); // Close after post
        });
    }

    private void showMediaPickerDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.social_dialog_media_picker);

        ImageButton btnGallery = dialog.findViewById(R.id.btn_gallery);
        ImageButton btnCamera = dialog.findViewById(R.id.btn_camera);

        btnGallery.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, REQUEST_GALLERY);
            dialog.dismiss();
        });

        btnCamera.setOnClickListener(v -> {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            startActivityForResult(intent, REQUEST_CAMERA);
            dialog.dismiss();
        });

        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == REQUEST_GALLERY || requestCode == REQUEST_CAMERA) {
                selectedMediaUri = data.getData();

                // Check the media type
                String mimeType = getContentResolver().getType(selectedMediaUri);
                if (mimeType != null && mimeType.startsWith("image")) {
                    // If it's an image, show the image preview
                    mediaPreviewImage.setVisibility(View.VISIBLE);
                    mediaPreviewVideo.setVisibility(View.GONE);
                    mediaPreviewImage.setImageURI(selectedMediaUri);
                } else if (mimeType != null && mimeType.startsWith("video")) {
                    // If it's a video, show the video preview
                    mediaPreviewVideo.setVisibility(View.VISIBLE);
                    mediaPreviewImage.setVisibility(View.GONE);
                    mediaPreviewVideo.setVideoURI(selectedMediaUri);
                    mediaPreviewVideo.start(); // Start playing the video
                }

                // Show the media preview container
                mediaContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    private void savePostToFirestore(String content) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
        String username = auth.getCurrentUser() != null ? auth.getCurrentUser().getDisplayName() : "Anonymous";

        Map<String, Object> post = new HashMap<>();
        post.put("userId", userId);
        post.put("username", username);
        post.put("content", content);
        post.put("isPublic", isPublic);
        post.put("timestamp", System.currentTimeMillis());
        post.put("likeCount", 0);
        post.put("commentCount", 0);

        if (selectedMediaUri != null) {
            // Upload media to Firebase Storage
            StorageReference mediaRef = storage.getReference().child("post_media/" + userId + "/" + System.currentTimeMillis());
            mediaRef.putFile(selectedMediaUri)
                    .addOnSuccessListener(taskSnapshot -> mediaRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        post.put("mediaUrl", uri.toString());
                        savePost(post);
                    }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to upload media", Toast.LENGTH_SHORT).show();
                    });
        } else {
            post.put("mediaUrl", "");
            savePost(post);
        }
    }

    private void savePost(Map<String, Object> post) {
        db.collection("posts").add(post)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Post created (" + (isPublic ? "Public" : "Private") + ")", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to create post", Toast.LENGTH_SHORT).show();
                });
    }
}
