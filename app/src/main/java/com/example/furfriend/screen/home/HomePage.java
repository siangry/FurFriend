package com.example.furfriend.screen.home;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.furfriend.Constants;
import com.example.furfriend.R;
import com.example.furfriend.screen.calendar.AllRemindersPage;
import com.example.furfriend.screen.profile.ViewAllPetPage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;

public class HomePage extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private LinearLayout petListLayout, reminderLayout, noPetView, noReminderView, seeMorePet, seeMoreReminder;
    private ImageView petImageView;
    private int[] backgroundColors;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.screen_home_page, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        petListLayout = view.findViewById(R.id.petListLayout);
        reminderLayout = view.findViewById(R.id.reminderLayout);
        noPetView = view.findViewById(R.id.noPetView);
        noReminderView = view.findViewById(R.id.noReminderView);
        seeMorePet = view.findViewById(R.id.seeMorePets);
        seeMoreReminder = view.findViewById(R.id.seeMoreReminders);
        TextView welcomeText = view.findViewById(R.id.welcomeText);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            db.collection(Constants.USERS)
                    .document(uid).collection(Constants.PROFILE)
                    .document(Constants.USER_DETAILS)
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

        seeMorePet.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ViewAllPetPage.class));
        });

        seeMoreReminder.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AllRemindersPage.class));
        });

        return view;

    }

    private void fetchPets() {
        backgroundColors = new int[]{
                ContextCompat.getColor(requireContext(), R.color.blue),
                ContextCompat.getColor(requireContext(), R.color.yellow),
                ContextCompat.getColor(requireContext(), R.color.pink),
                ContextCompat.getColor(requireContext(), R.color.green)
        };
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            db.collection(Constants.USERS)
                    .document(currentUser.getUid())
                    .collection(Constants.PET)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            noPetView.setVisibility(View.GONE);
                            petListLayout.setVisibility(View.VISIBLE);

                            petListLayout.removeAllViews();

                            for (QueryDocumentSnapshot documentSnapshot : querySnapshot) {
                                String petName = documentSnapshot.getString("petName");
                                String petType = documentSnapshot.getString("type");
                                String imageBase64 = documentSnapshot.getString("petPictureBase64");

                                View petView = LayoutInflater.from(getContext())
                                        .inflate(R.layout.pet_item, petListLayout, false);

                                TextView nameTextView = petView.findViewById(R.id.petName);
                                TextView typeTextView = petView.findViewById(R.id.petType);
                                ImageView petImageView = petView.findViewById(R.id.petImage);

                                nameTextView.setText(petName != null ? petName : "Unknown Name");
                                typeTextView.setText(petType != null ? petType : "Unknown Type");

                                if (imageBase64 != null && !imageBase64.isEmpty()) {
                                    byte[] decodedBytes = Base64.decode(imageBase64, Base64.DEFAULT);

                                    Glide.with(this)
                                            .asBitmap()
                                            .load(decodedBytes)
                                            .circleCrop()
                                            .into(petImageView);
                                } else {
                                    petImageView.setImageResource(R.drawable.ic_animal_image);
                                }

                                Random random = new Random();
                                int randomColor = backgroundColors[random.nextInt(backgroundColors.length)];
                                GradientDrawable backgroundDrawable = new GradientDrawable();
                                backgroundDrawable.setColor(randomColor);
                                backgroundDrawable.setCornerRadius(30f);
                                petView.setBackground(backgroundDrawable);

                                petListLayout.addView(petView);
                            }
                        } else {
                            petListLayout.setVisibility(View.GONE);
                            noPetView.setVisibility(View.VISIBLE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        petListLayout.setVisibility(View.GONE);
                        noPetView.setVisibility(View.VISIBLE);
                    });
        } else {
            petListLayout.setVisibility(View.GONE);
            noPetView.setVisibility(View.VISIBLE);
        }
    }

    private void fetchTodayReminders() {
        backgroundColors = new int[]{
                ContextCompat.getColor(requireContext(), R.color.blue),
                ContextCompat.getColor(requireContext(), R.color.yellow),
                ContextCompat.getColor(requireContext(), R.color.pink),
                ContextCompat.getColor(requireContext(), R.color.green)
        };

        String userId = mAuth.getCurrentUser().getUid();
        String todayDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date());

        db.collection("reminders")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", todayDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    reminderLayout.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        noReminderView.setVisibility(View.VISIBLE);
                        reminderLayout.setVisibility(View.INVISIBLE);
                    } else {
                        noReminderView.setVisibility(View.INVISIBLE);
                        reminderLayout.setVisibility(View.VISIBLE);

                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            String title = doc.getString("title");
                            String date = doc.getString("date");
                            String time = doc.getString("time");

                            View reminderView = LayoutInflater.from(getContext())
                                    .inflate(R.layout.reminder_item, reminderLayout, false);

                            TextView titleTextView = reminderView.findViewById(R.id.reminderTitle);
                            TextView dateTextView = reminderView.findViewById(R.id.reminderDate);
                            TextView timeTextView = reminderView.findViewById(R.id.reminderTime);

                            titleTextView.setText(title != null ? title : getString(R.string.unknown));
                            dateTextView.setText(date != null ? date : getString(R.string.unknown));
                            timeTextView.setText(time != null ? time : getString(R.string.unknown));

                            Random random = new Random();
                            int randomColor = backgroundColors[random.nextInt(backgroundColors.length)];
                            GradientDrawable backgroundDrawable = new GradientDrawable();
                            backgroundDrawable.setColor(randomColor);
                            backgroundDrawable.setCornerRadius(30f);
                            reminderView.setBackground(backgroundDrawable);

                            reminderLayout.addView(reminderView);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    noReminderView.setVisibility(View.VISIBLE);
                    reminderLayout.setVisibility(View.INVISIBLE);
                });
    }
}
