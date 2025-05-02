package com.example.furfriend.screen.social;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.furfriend.BaseActivity;
import com.example.furfriend.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AddPostActivity extends BaseActivity {

    private static final String TAG = "AddPostActivity";
    private static final int REQUEST_GALLERY = 1001;
    private static final int REQUEST_CAMERA = 1002;
    private static final int CAMERA_PERMISSION_REQUEST = 1003;
    private static final int STORAGE_PERMISSION_REQUEST = 1004;
    private static final int REQUEST_SETTINGS = 1005;

    private boolean isPublic = true;
    private EditText postContent;
    private ImageView mediaPreviewImage;
    private VideoView mediaPreviewVideo;
    private Uri selectedMediaUri = null;
    private FrameLayout mediaContainer;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private FirebaseAuth auth;
    private Uri cameraImageUri;
    private boolean awaitingCameraPermission = false;
    private boolean awaitingStoragePermission = false;

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
        TextView tv_public = findViewById(R.id.txt_addpost_public);
        TextView tv_private = findViewById(R.id.txt_addpost_private);
        postContent = findViewById(R.id.social_input_post);
        mediaPreviewImage = findViewById(R.id.social_image_preview);
        mediaPreviewVideo = findViewById(R.id.social_video_preview);
        mediaContainer = findViewById(R.id.social_media_preview_container);

        // Set default state for Public button
        btnPublic.setImageResource(R.drawable.ic_social_addpost_public2);  // Public selected by default
        btnPrivate.setImageResource(R.drawable.ic_social_addpost_private1);  // Private deselected
        tv_private.setSelected(false);

        // Back
        btnBack.setOnClickListener(v -> finish());

        // Visibility toggle
        btnPublic.setOnClickListener(v -> {
            isPublic = true;
            btnPublic.setImageResource(R.drawable.ic_social_addpost_public2);
            tv_public.setSelected(true);
            btnPrivate.setImageResource(R.drawable.ic_social_addpost_private1);
            tv_private.setSelected(false);
        });

        btnPrivate.setOnClickListener(v -> {
            isPublic = false;
            btnPublic.setImageResource(R.drawable.ic_social_addpost_public1);
            tv_public.setSelected(false);
            btnPrivate.setImageResource(R.drawable.ic_social_addpost_private2);
            tv_private.setSelected(true);
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

            // For Android 10+, we don't need storage permission for gallery access
            if (selectedMediaUri != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !checkStoragePermission()) {
                awaitingStoragePermission = true;
                requestStoragePermission();
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
            // No storage permission needed for gallery access on Android 10+
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && !checkStoragePermission()) {
                awaitingStoragePermission = true;
                requestStoragePermission();
            } else {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, REQUEST_GALLERY);
            }
            dialog.dismiss();
        });

        btnCamera.setOnClickListener(v -> {
            if (checkCameraPermission()) {
                openCamera();
            } else {
                awaitingCameraPermission = true;
                requestCameraPermission();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED);
    }

    private boolean checkStoragePermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ?
                        new String[]{Manifest.permission.CAMERA} :
                        new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                CAMERA_PERMISSION_REQUEST);
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                STORAGE_PERMISSION_REQUEST);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || (grantResults.length > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED))) {
                if (awaitingCameraPermission) {
                    openCamera();
                    awaitingCameraPermission = false;
                }
            } else {
                awaitingCameraPermission = false;
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    Toast.makeText(this, "Camera permission denied. Please enable it in settings.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_SETTINGS);
                } else {
                    Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (awaitingStoragePermission) {
                    if (selectedMediaUri == null) {
                        showMediaPickerDialog();
                    } else {
                        savePostToFirestore(postContent.getText().toString().trim());
                    }
                    awaitingStoragePermission = false;
                }
            } else {
                awaitingStoragePermission = false;
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Toast.makeText(this, "Storage permission denied. Please enable it in settings.", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_SETTINGS);
                } else {
                    Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SETTINGS) {
            if (awaitingCameraPermission && checkCameraPermission()) {
                openCamera();
                awaitingCameraPermission = false;
            } else if (awaitingStoragePermission && checkStoragePermission()) {
                if (selectedMediaUri == null) {
                    showMediaPickerDialog();
                } else {
                    savePostToFirestore(postContent.getText().toString().trim());
                }
                awaitingStoragePermission = false;
            }
            return;
        }

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_GALLERY) {
                selectedMediaUri = data.getData();
            } else if (requestCode == REQUEST_CAMERA) {
                selectedMediaUri = cameraImageUri;
            }

            if (selectedMediaUri != null) {
                Log.d(TAG, "Selected media URI: " + selectedMediaUri.toString());
                String mimeType = getContentResolver().getType(selectedMediaUri);
                if (mimeType != null && mimeType.startsWith("image")) {
                    mediaPreviewImage.setVisibility(View.VISIBLE);
                    mediaPreviewVideo.setVisibility(View.GONE);
                    mediaPreviewImage.setImageURI(selectedMediaUri);
                } else if (mimeType != null && mimeType.startsWith("video")) {
                    mediaPreviewVideo.setVisibility(View.VISIBLE);
                    mediaPreviewImage.setVisibility(View.GONE);
                    mediaPreviewVideo.setVideoURI(selectedMediaUri);
                    mediaPreviewVideo.start();
                }

                mediaContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
                return;
            }
            if (photoFile != null) {
                cameraImageUri = FileProvider.getUriForFile(this,
                        "com.example.furfriend.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                startActivityForResult(takePictureIntent, REQUEST_CAMERA);
            }
        }
    }

    private File createImageFile() throws IOException {
        String imageFileName = "JPEG_" + System.currentTimeMillis() + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
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
            try {
                Log.d(TAG, "Converting media to Base64: " + selectedMediaUri.toString());
                String base64Media = convertMediaToBase64(selectedMediaUri);
                if (base64Media.isEmpty()) {
                    Toast.makeText(this, "Media type not supported (videos not supported)", Toast.LENGTH_SHORT).show();
                    post.put("mediaUrl", "");
                } else {
                    post.put("mediaUrl", base64Media);
                    Log.d(TAG, "Media converted to Base64, length: " + base64Media.length());
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to convert media to Base64", e);
                Toast.makeText(this, "Failed to process media: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                post.put("mediaUrl", "");
            }
        } else {
            post.put("mediaUrl", "");
        }

        savePost(post);
    }

    private String convertMediaToBase64(Uri uri) throws IOException {
        if (uri == null) return "";

        String mimeType = getContentResolver().getType(uri);
        if (mimeType != null && mimeType.startsWith("image")) {
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
            } else {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            }

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream); // 80% quality
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } else {
            // Videos are not supported for Base64 conversion due to size constraints
            return "";
        }
    }

    private void savePost(Map<String, Object> post) {
        db.collection("posts").add(post)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Post created successfully with ID: " + documentReference.getId());
                    Toast.makeText(this, "Post created (" + (isPublic ? "Public" : "Private") + ")", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create post", e);
                    Toast.makeText(this, "Failed to create post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}