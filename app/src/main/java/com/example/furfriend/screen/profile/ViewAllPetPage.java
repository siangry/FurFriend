package com.example.furfriend.screen.profile;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.furfriend.FirestoreCollection;
import com.example.furfriend.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class ViewAllPetPage extends AppCompatActivity {
    private RecyclerView recyclerView;
    private TextView textViewNoPet;
    private PetAdapter petAdapter;
    private List<Pet> petList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_all_pet_page);

        recyclerView = findViewById(R.id.petList);
        textViewNoPet = findViewById(R.id.noPetView);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        petList = new ArrayList<>();
        petAdapter = new PetAdapter(this, petList);
        recyclerView.setAdapter(petAdapter);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        fetchPetsFromFirestore();
    }

    private void fetchPetsFromFirestore() {
        String userId = mAuth.getCurrentUser().getUid();
        CollectionReference petsRef = db.collection(FirestoreCollection.USERS).document(userId).collection(FirestoreCollection.PET);

        petsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot querySnapshot = task.getResult();
                if (querySnapshot != null && !querySnapshot.isEmpty()) {
                    petList.clear();
                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String name = document.getString("name");
                        String type = document.getString("type");
                        String age = document.getString("age");
                        String gender = document.getString("gender");
                        String imageBase64 = document.getString("petImage");

                        Pet pet = new Pet(name, type, age, gender, imageBase64);
                        petList.add(pet);
                    }
                    textViewNoPet.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                } else {
                    // No pets
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