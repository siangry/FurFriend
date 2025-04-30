package com.example.furfriend.screen.profile;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.furfriend.BaseActivity;
import com.example.furfriend.Constants;
import com.example.furfriend.R;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ViewAllPetPage extends BaseActivity {
    private RecyclerView recyclerView;
    private TextView textViewNoPet;
    private PetAdapter petAdapter;
    private List<Pet> petList;
    private ImageView btnBack, editPetDetailsView;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_all_pet_page);

        recyclerView = findViewById(R.id.petList);
        textViewNoPet = findViewById(R.id.noPetView);
        btnBack = findViewById(R.id.btnBack);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        String userId = mAuth.getCurrentUser().getUid();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        petList = new ArrayList<>();
        petAdapter = new PetAdapter(this, petList, userId);
        recyclerView.setAdapter(petAdapter);

        fetchPetsFromFirestore();

        findViewById(R.id.btnAddNewPet).setOnClickListener(v -> {
            startActivity(new Intent(ViewAllPetPage.this, AddPetPage.class));
        });

        btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });
    }

    private void fetchPetsFromFirestore() {
        String userId = mAuth.getCurrentUser().getUid();
        CollectionReference petsRef = db.collection(Constants.USERS)
                .document(userId).collection(Constants.PET);

        petsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    petList.clear();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String name = document.getString("petName");
                        String type = document.getString("type");
                        Long ageLong = document.getLong("age");
                        int age = (ageLong != null) ? ageLong.intValue() : 0;
                        String ageUnit = document.getString("ageUnit");
                        String gender = document.getString("gender");
                        String imageBase64 = document.getString("petPictureBase64");
                        Double weightDouble = document.getDouble("weight");
                        String petWeight = (weightDouble != null) ? String.valueOf(weightDouble) : "0.0";

                        String petId = document.getId();
                        Pet pet = new Pet(name, type, age, ageUnit, gender, imageBase64, petWeight, petId);
                        petList.add(pet);
                    }
                    textViewNoPet.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    textViewNoPet.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                }
                petAdapter.notifyDataSetChanged();
            } else {
                textViewNoPet.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            }
        });
    }
}