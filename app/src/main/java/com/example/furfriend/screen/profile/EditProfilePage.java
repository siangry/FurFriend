package com.example.furfriend.screen.profile;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.furfriend.BaseActivity;
import com.example.furfriend.Constants;
import com.example.furfriend.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EditProfilePage extends BaseActivity {

    private ImageView imageViewProfile, uploadProfileImage, backButton;
    private EditText editTextUsername;
    private TextView textViewEmail;
    private Button buttonSave;
    private Uri imageUri;
    private Bitmap selectedBitmap;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    private ActivityResultLauncher<Intent> imagePickerLauncher;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile_page);

        imageViewProfile = findViewById(R.id.profileImage);
        uploadProfileImage = findViewById(R.id.btnUploadImage);
        editTextUsername = findViewById(R.id.editUsername);
        textViewEmail = findViewById(R.id.userEmail);
        buttonSave = findViewById(R.id.btnSaveChanges);
        backButton = findViewById(R.id.btnBack);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri dataUri = result.getData().getData();
                        try {
                            Bitmap originalBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), dataUri);

                            selectedBitmap = Bitmap.createScaledBitmap(originalBitmap, 300, 300, true);

                            Glide.with(this)
                                    .load(selectedBitmap)
                                    .circleCrop()
                                    .into(imageViewProfile);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }

        loadUserData();

        uploadProfileImage.setOnClickListener(v -> chooseImage());
        buttonSave.setOnClickListener(v -> uploadData());
        backButton.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });

    }

    private void loadUserData() {
        db.collection(Constants.USERS)
                .document(userId).collection(Constants.PROFILE)
                .document(Constants.USER_DETAILS)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String username = documentSnapshot.getString("username");
                        String email = documentSnapshot.getString("email");
                        String base64Image = documentSnapshot.getString("profilePictureBase64");

                        editTextUsername.setText(username);
                        textViewEmail.setText(email);

                        if (base64Image != null && !base64Image.isEmpty()) {
                            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);

                            Glide.with(this)
                                    .asBitmap()
                                    .load(decodedBytes)
                                    .circleCrop()
                                    .into(imageViewProfile);
                        }
                    }
                });
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imagePickerLauncher.launch(Intent.createChooser(intent, getString(R.string.selectPicture)));
    }

    private void uploadData() {
        String username = editTextUsername.getText().toString().trim();
        if (username.isEmpty()) {
            editTextUsername.setError(getString(R.string.usernameRequired));
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle(getString(R.string.saving));
        progressDialog.show();

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("username", username);

        if (selectedBitmap != null) {
            String base64Image = encodeImage(selectedBitmap);
            userUpdates.put("profilePictureBase64", base64Image);
        }

        db.collection(Constants.USERS)
                .document(userId).collection(Constants.PROFILE)
                .document(Constants.USER_DETAILS)
                .update(userUpdates)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Intent intent = new Intent(EditProfilePage.this, UpdateSuccessPage.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(EditProfilePage.this, getString(R.string.failToUpdateProfile), Toast.LENGTH_SHORT).show();
                });
    }

    private String encodeImage(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 30, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }

    private void loadBase64Image(String base64String) {
        byte[] decodedBytes = Base64.decode(base64String, Base64.DEFAULT);
        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        imageViewProfile.setImageBitmap(decodedBitmap);
    }
}