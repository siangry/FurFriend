package com.example.furfriend.screen.calendar;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.furfriend.Constants;
import com.example.furfriend.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditActivityPage extends AppCompatActivity {
    private EditText titleInput, descriptionInput, dateInput, timeInput;
    private Button saveButton;
    private ImageView btnBack;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String activityId;
    private RecyclerView rvPets;
    private PetSelectionAdapter petAdapter;
    private LinearLayout petSelectionHeader;
    private TextView tvSelectedPets;
    private ImageView ivDropdownArrow;
    private boolean isPetListExpanded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_activity);

        // Initialize Firestore and Auth
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        initializeViews();
        setupClickListeners();
        loadActivityData();
        fetchPets();
    }

    private void initializeViews() {
        titleInput = findViewById(R.id.titleInput);
        descriptionInput = findViewById(R.id.descriptionInput);
        dateInput = findViewById(R.id.dateInput);
        timeInput = findViewById(R.id.timeInput);
        saveButton = findViewById(R.id.saveButton);
        btnBack = findViewById(R.id.btnBack);
        rvPets = findViewById(R.id.rvPets);
        petSelectionHeader = findViewById(R.id.petSelectionHeader);
        tvSelectedPets = findViewById(R.id.tvSelectedPets);
        ivDropdownArrow = findViewById(R.id.ivDropdownArrow);

        // Set up RecyclerView
        rvPets.setLayoutManager(new LinearLayoutManager(this));

        // Get activity ID from intent
        activityId = getIntent().getStringExtra("activityId");
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveActivity());
        dateInput.setOnClickListener(v -> showDatePicker());
        timeInput.setOnClickListener(v -> showTimePicker());
        petSelectionHeader.setOnClickListener(v -> togglePetList());
    }

    private void togglePetList() {
        isPetListExpanded = !isPetListExpanded;
        rvPets.setVisibility(isPetListExpanded ? View.VISIBLE : View.GONE);

        // Rotate arrow icon
        float rotation = isPetListExpanded ? 180f : 0f;
        RotateAnimation rotateAnimation = new RotateAnimation(
            isPetListExpanded ? 0f : 180f,
            rotation,
            RotateAnimation.RELATIVE_TO_SELF,
            0.5f,
            RotateAnimation.RELATIVE_TO_SELF,
            0.5f
        );
        rotateAnimation.setDuration(200);
        rotateAnimation.setFillAfter(true);
        ivDropdownArrow.startAnimation(rotateAnimation);
    }

    private void updateSelectedPetsText() {
        List<PetSelectionAdapter.PetItem> selectedPets = petAdapter.getSelectedPets();
        if (selectedPets.isEmpty()) {
            tvSelectedPets.setText("Select pets");
        } else {
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < selectedPets.size(); i++) {
                if (i > 0) {
                    text.append(", ");
                }
                text.append(selectedPets.get(i).getName());
            }
            tvSelectedPets.setText(text.toString());
        }
    }

    private void loadActivityData() {
        if (activityId != null) {
            db.collection("activities")
                .document(activityId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> activity = documentSnapshot.getData();
                        if (activity != null) {
                            titleInput.setText((String) activity.get("title"));
                            descriptionInput.setText((String) activity.get("description"));
                            dateInput.setText((String) activity.get("date"));
                            timeInput.setText((String) activity.get("time"));
                            
                            // Load selected pets
                            List<Map<String, String>> pets = (List<Map<String, String>>) activity.get("pets");
                            if (pets != null && petAdapter != null) {
                                for (Map<String, String> pet : pets) {
                                    petAdapter.selectPet(pet.get("petId"));
                                }
                                updateSelectedPetsText();
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(EditActivityPage.this, "Error loading activity: " + e.getMessage(),
                                 Toast.LENGTH_SHORT).show();
                });
        }
    }

    private void fetchPets() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection(Constants.USERS)
            .document(userId)
            .collection(Constants.PET)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<PetSelectionAdapter.PetItem> pets = new ArrayList<>();

                    for (QueryDocumentSnapshot document : task.getResult()) {
                        String petName = document.getString("petName");
                        if (petName != null) {
                            pets.add(new PetSelectionAdapter.PetItem(document.getId(), petName));
                        }
                    }

                    // Setup pet adapter
                    petAdapter = new PetSelectionAdapter(pets);
                    petAdapter.setOnPetSelectionChangeListener(this::updateSelectedPetsText);
                    rvPets.setAdapter(petAdapter);

                    // Reload activity data to set selected pets
                    loadActivityData();
                } else {
                    Toast.makeText(EditActivityPage.this, "Error loading pets: " + task.getException().getMessage(),
                                 Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                dateInput.setText(dateFormat.format(calendar.getTime()));
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                calendar.set(Calendar.MINUTE, minute);
                SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                timeInput.setText(timeFormat.format(calendar.getTime()));
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        );
        timePickerDialog.show();
    }

    private void saveActivity() {
        String title = titleInput.getText().toString().trim();
        String description = descriptionInput.getText().toString().trim();
        String date = dateInput.getText().toString().trim();
        String time = timeInput.getText().toString().trim();
        List<PetSelectionAdapter.PetItem> selectedPets = petAdapter.getSelectedPets();

        if (title.isEmpty()) {
            titleInput.setError("Title is required");
            return;
        }

        if (selectedPets.isEmpty()) {
            Toast.makeText(this, "Please select at least one pet", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create activity data
        Map<String, Object> activity = new HashMap<>();
        activity.put("title", title);
        activity.put("description", description);
        activity.put("date", date);
        activity.put("time", time);
        activity.put("userId", mAuth.getCurrentUser().getUid());
        
        // Add selected pets
        List<Map<String, String>> pets = new ArrayList<>();
        for (PetSelectionAdapter.PetItem pet : selectedPets) {
            Map<String, String> petData = new HashMap<>();
            petData.put("petId", pet.getId());
            petData.put("petName", pet.getName());
            pets.add(petData);
        }
        activity.put("pets", pets);

        // Update in Firestore
        db.collection("activities")
            .document(activityId)
            .update(activity)
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(EditActivityPage.this, "Activity updated successfully",
                             Toast.LENGTH_SHORT).show();
                finish();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(EditActivityPage.this, "Error updating activity: " + e.getMessage(),
                             Toast.LENGTH_SHORT).show();
            });
    }
} 