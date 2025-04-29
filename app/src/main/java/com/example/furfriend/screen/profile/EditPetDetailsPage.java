package com.example.furfriend.screen.profile;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;

import com.bumptech.glide.Glide;
import com.example.furfriend.BaseActivity;
import com.example.furfriend.Constants;
import com.example.furfriend.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class EditPetDetailsPage extends BaseActivity {

    private ImageView animalImage, btnBack;
    private TextView petNameTitle, removePetTextView;
    private EditText showPetName, showAge, showWeight;
    private Spinner showTypeSpinner, showAgeSpinner, showGenderSpinner;
    private Button btnSaveChanges;

    private FirebaseFirestore db;
    private String userId, petId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_pet_details_page);

        petNameTitle = findViewById(R.id.petNameTitle);
        animalImage = findViewById(R.id.animalImage);
        showPetName = findViewById(R.id.showPetName);
        showAge = findViewById(R.id.showAge);
        showWeight = findViewById(R.id.showWeight);
        showTypeSpinner = findViewById(R.id.showTypeSpinner);
        showAgeSpinner = findViewById(R.id.showAgeSpinner);
        showGenderSpinner = findViewById(R.id.showGenderSpinner);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnBack = findViewById(R.id.btnBack);
        removePetTextView = findViewById(R.id.removePet);

        String[] types = {getString(R.string.dog), getString(R.string.cat), getString(R.string.hamster), getString(R.string.rabbit), getString(R.string.bird), getString(R.string.fish), getString(R.string.reptile), getString(R.string.other)};
        String[] ageUnit = {getString(R.string.day), getString(R.string.week), getString(R.string.month), getString(R.string.year)};
        String[] gender = {getString(R.string.male), getString(R.string.female)};

        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, types);
        typeAdapter.setDropDownViewResource(R.layout.spinner_option_item);
        showTypeSpinner.setAdapter(typeAdapter);

        ArrayAdapter<String> ageUnitAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, ageUnit);
        ageUnitAdapter.setDropDownViewResource(R.layout.spinner_option_item);
        showAgeSpinner.setAdapter(ageUnitAdapter);

        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, gender);
        genderAdapter.setDropDownViewResource(R.layout.spinner_option_item);
        showGenderSpinner.setAdapter(genderAdapter);

        userId = getIntent().getStringExtra("userId");
        petId = getIntent().getStringExtra("petId");

        db = FirebaseFirestore.getInstance();

        fetchPetDetails();

        btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });
        btnSaveChanges.setOnClickListener(v -> {
            saveChanges();
        });
        removePetTextView.setOnClickListener(v -> {
            showRemoveBottomSheet();
        });

    }

    private void fetchPetDetails() {
        db.collection(Constants.USERS)
                .document(userId)
                .collection(Constants.PET)
                .document(petId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String petName = documentSnapshot.getString("petName");
                        String petType = documentSnapshot.getString("type");
                        Long ageLong = documentSnapshot.getLong("age");
                        int age = (ageLong != null) ? ageLong.intValue() : 0;
                        String ageUnit = documentSnapshot.getString("ageUnit");
                        String gender = documentSnapshot.getString("gender");
                        Double weightDouble = documentSnapshot.getDouble("weight");
                        String petWeight = (weightDouble != null) ? String.valueOf(weightDouble) : "0.0";
                        String imageBase64 = documentSnapshot.getString("petPictureBase64");

                        petNameTitle.setText(petName != null ? petName : "");
                        showPetName.setText(petName != null ? petName : "");
                        showAge.setText(String.valueOf(age));
                        showWeight.setText(petWeight);

                        if (petType != null) setSpinnerSelection(showTypeSpinner, petType);
                        if (ageUnit != null) setSpinnerSelection(showAgeSpinner, ageUnit);
                        if (gender != null) setSpinnerSelection(showGenderSpinner, gender);

                        if (imageBase64 != null && !imageBase64.isEmpty()) {
                            byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);

                            Glide.with(this)
                                    .asBitmap()
                                    .load(decodedBytes)
                                    .circleCrop()
                                    .into(animalImage);
                        } else {
                            animalImage.setImageResource(R.drawable.ic_animal_image);
                        }
                    } else {
                        Toast.makeText(this, "No pet details found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditPetDetailsPage.this, "Failed to load pet details.", Toast.LENGTH_SHORT).show();
                });
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        if (adapter != null) {
            int position = adapter.getPosition(value);
            if (position >= 0) {
                spinner.setSelection(position);
            }
        }
    }

    private void saveChanges() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Saving changes...");
        progressDialog.show();

        String updatedName = showPetName.getText().toString().trim();
        String updatedType = showTypeSpinner.getSelectedItem().toString();
        String updatedAge = showAge.getText().toString().trim();
        String updatedAgeUnit = showAgeSpinner.getSelectedItem().toString();
        String updatedGender = showGenderSpinner.getSelectedItem().toString();
        String updatedWeight = showWeight.getText().toString().trim();

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("petName", updatedName);
        updatedData.put("type", updatedType);
        updatedData.put("age", Integer.parseInt(updatedAge));
        updatedData.put("ageUnit", updatedAgeUnit);
        updatedData.put("gender", updatedGender);
        updatedData.put("weight", Double.parseDouble(updatedWeight));

        DocumentReference petRef = db.collection(Constants.USERS)
                .document(userId)
                .collection(Constants.PET)
                .document(petId);

        petRef.update(updatedData)
                .addOnSuccessListener(unused -> {
                    progressDialog.dismiss();
                    Toast.makeText(EditPetDetailsPage.this, "Changes saved successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Finish activity
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(EditPetDetailsPage.this, "Failed to save changes.", Toast.LENGTH_SHORT).show();
                });
    }

    private void showRemoveBottomSheet() {
        if (isFinishing()) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.remove_pet_bottom_sheet, null);
        if (sheetView == null) return;

        bottomSheetDialog.setContentView(sheetView);

        Button confirmDelete = sheetView.findViewById(R.id.btnRemove);
        TextView cancelDelete = sheetView.findViewById(R.id.btnCancel);

        confirmDelete.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            deletePet();
        });

        cancelDelete.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void deletePet() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Deleting pet...");
        progressDialog.show();

        DocumentReference petRef = db.collection(Constants.USERS)
                .document(userId)
                .collection(Constants.PET)
                .document(petId);

        petRef.delete()
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(EditPetDetailsPage.this, "Pet deleted successfully", Toast.LENGTH_SHORT).show();
                    finish();  // Close the activity and go back to the previous screen
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(EditPetDetailsPage.this, "Failed to delete pet", Toast.LENGTH_SHORT).show();
                });
    }


}