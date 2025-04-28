package com.example.furfriend.screen.profile;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.furfriend.FirestoreCollection;
import com.example.furfriend.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AddPetPage extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private EditText etPetName, etAge, etWeight;
    private Spinner spinnerType, spinnerAgeUnit, spinnerGender;
    private Button btnAddPet;
    private ImageView btnBack, uploadProfileImage, animalImage;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Bitmap selectedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pet_page);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        etPetName = findViewById(R.id.addPetName);
        etAge = findViewById(R.id.addAge);
        etWeight = findViewById(R.id.addWeight);
        spinnerType = findViewById(R.id.typeSpinner);
        spinnerAgeUnit = findViewById(R.id.ageSpinner);
        spinnerGender = findViewById(R.id.genderSpinner);
        btnAddPet = findViewById(R.id.btnAddNewPet);
        uploadProfileImage = findViewById(R.id.btnUploadImage);
        animalImage = findViewById(R.id.animalImage);
        btnBack = findViewById(R.id.btnBack);

        String[] types = {getString(R.string.dog), getString(R.string.cat), getString(R.string.hamster), getString(R.string.rabbit), getString(R.string.bird), getString(R.string.fish), getString(R.string.reptile), getString(R.string.other)};
        String[] ageUnit = {getString(R.string.day), getString(R.string.week), getString(R.string.month), getString(R.string.year)};
        String[] gender = {getString(R.string.male), getString(R.string.female)};

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, types);
        typeAdapter.setDropDownViewResource(R.layout.spinner_option_item);
        spinnerType.setAdapter(typeAdapter);

        ArrayAdapter<String> ageUnitAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, ageUnit);
        ageUnitAdapter.setDropDownViewResource(R.layout.spinner_option_item);
        spinnerAgeUnit.setAdapter(ageUnitAdapter);

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, gender);
        genderAdapter.setDropDownViewResource(R.layout.spinner_option_item);
        spinnerGender.setAdapter(genderAdapter);

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
                                    .into(animalImage);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );


        uploadProfileImage.setOnClickListener(v -> chooseImage());
        btnAddPet.setOnClickListener(v -> addPet());
        btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });
    }

    private void chooseImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Picture"));
    }

    private void addPet() {
        String petName = etPetName.getText().toString().trim();
        String type = spinnerType.getSelectedItem().toString();
        String age = etAge.getText().toString().trim();
        String ageUnit = spinnerAgeUnit.getSelectedItem().toString();
        String gender = spinnerGender.getSelectedItem().toString();
        String weight = etWeight.getText().toString().trim();

        if (petName.isEmpty() || age.isEmpty() || weight.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> pet = new HashMap<>();
        pet.put("petName", petName);
        pet.put("type", type);
        pet.put("age", Integer.parseInt(age));
        pet.put("ageUnit", ageUnit);
        pet.put("gender", gender);
        pet.put("weight", Double.parseDouble(weight));

        if (selectedBitmap != null) {
            String base64Image = encodeImage(selectedBitmap);
            pet.put("petPictureBase64", base64Image);
        }

        db.collection(FirestoreCollection.USERS)
                .document(userId)
                .collection(FirestoreCollection.PET)
                .add(pet)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(AddPetPage.this, "Pet added successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AddPetPage.this, "Failed to add pet", Toast.LENGTH_SHORT).show();
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
        animalImage.setImageBitmap(decodedBitmap);
    }
}