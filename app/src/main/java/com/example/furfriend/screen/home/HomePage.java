package com.example.furfriend.screen.home;

import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.furfriend.FirestoreCollection;
import com.example.furfriend.R;
import com.example.furfriend.screen.loginSignup.LoginActivity;
import com.example.furfriend.screen.loginSignup.SignupActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class HomePage extends Fragment {

    private FirebaseFirestore db;
    private LinearLayout petListLayout, reminderLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.screen_home_page, container, false);

        db = FirebaseFirestore.getInstance();

        petListLayout = view.findViewById(R.id.petListLayout);
        reminderLayout = view.findViewById(R.id.reminderLayout);
        TextView welcomeText = view.findViewById(R.id.welcomeText);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            db.collection(FirestoreCollection.USERS)
                    .document(uid).collection(FirestoreCollection.PROFILE)
                    .document(FirestoreCollection.USER_DETAILS)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");

                            if (username != null && !username.isEmpty()) {
                                welcomeText.setText(getString(R.string.hi, username));
                            } else {
                                welcomeText.setText(getString(R.string.hi, "user"));
                            }
                        } else {
                            welcomeText.setText(getString(R.string.hi, "user"));
                        }
                    });
        }

        fetchPets();
        fetchTodayReminders();

        return view;

//        TextView seeMoreReminder = view.findViewById(R.id.seeMoreReminders);
//        seeMoreReminder.setOnClickListener(v -> {
//            navigateToSeeMoreReminder();
//        });
//
//        TextView seeMorePet = view.findViewById(R.id.seeMorePets);
//        seeMorePet.setOnClickListener(v -> {
//            navigateToSeeMorePet();
//        });
    }

    private void fetchPets() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            db.collection(FirestoreCollection.USERS).document(currentUser.getUid()).collection(FirestoreCollection.PET)
                    .whereEqualTo("userId", currentUser.getUid())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots.isEmpty()) {
                            TextView noPetText = new TextView(getContext());
                            noPetText.setText(R.string.noPetAdded);
                            petListLayout.addView(noPetText);
                        } else {
                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                String imageUrl = doc.getString("imageUrl");

                                ImageView petImage = new ImageView(getContext());
                                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(200, 200);
                                params.setMargins(16, 0, 16, 0);
                                petImage.setLayoutParams(params);
                                petImage.setScaleType(ImageView.ScaleType.CENTER_CROP);

                                Glide.with(requireContext()).load(imageUrl).into(petImage);

                                petListLayout.addView(petImage);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to load pets.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void fetchTodayReminders() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            db.collection(FirestoreCollection.USERS).document(currentUser.getUid()).collection(FirestoreCollection.REMINDER)
                    .whereEqualTo("userId", Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid())
                    .whereEqualTo("date", getTodayDate())
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots.isEmpty()) {
                            TextView noReminderText = new TextView(getContext());
                            noReminderText.setText(R.string.noReminderToday);
                            reminderLayout.addView(noReminderText);
                        } else {
                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                String title = doc.getString("title");
                                String time = doc.getString("time");

                                TextView reminderText = new TextView(getContext());
                                reminderText.setBackgroundResource(R.drawable.round_border);
                                reminderText.setPadding(24, 24, 24, 24);
                                reminderText.setText(title + "\n" + time);
                                reminderLayout.addView(reminderText);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to load reminders.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

//    private void navigateToSeeMoreReminder() {
//        Fragment seeMoreFragment = new SeeMoreFragment();
//        requireActivity().getSupportFragmentManager()
//                .beginTransaction()
//                .replace(R.id.fragment_container, seeMoreFragment)
//                .addToBackStack(null)
//                .commit();
//    }

//    private void navigateToSeeMorePet() {
//        Fragment seeMoreFragment = new SeeMoreFragment();
//        requireActivity().getSupportFragmentManager()
//                .beginTransaction()
//                .replace(R.id.fragment_container, seeMoreFragment)
//                .addToBackStack(null)
//                .commit();
//    }
}
