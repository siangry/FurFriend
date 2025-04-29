package com.example.furfriend.screen.profile;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.furfriend.FirestoreCollection;
import com.example.furfriend.R;
import com.example.furfriend.screen.loginSignup.LoginActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Random;

public class ProfilePage extends Fragment {
    private ImageView profileImageView, editProfileImageView, resetPassImageView;
    private TextView usernameTextView, emailTextView, logoutTextView;
    private LinearLayout petsContainer, noPetView, seeMorePet;
    private Button addPetButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private int[] backgroundColors;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.screen_profile_page, container, false);

        profileImageView = view.findViewById(R.id.profileImage);
        editProfileImageView = view.findViewById(R.id.editProfile);
        usernameTextView = view.findViewById(R.id.userName);
        emailTextView = view.findViewById(R.id.userEmail);
        petsContainer = view.findViewById(R.id.petContainer);
        noPetView = view.findViewById(R.id.noPetView);
        seeMorePet = view.findViewById(R.id.seeMorePets);
        addPetButton = view.findViewById(R.id.btnAddPet);
        logoutTextView = view.findViewById(R.id.logout);
        resetPassImageView = view.findViewById(R.id.resetPasswordProfile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserProfile();
        loadUserPets();

        addPetButton.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AddPetPage.class));
        });

        logoutTextView.setOnClickListener(v -> {
            showLogoutBottomSheet();
        });

        editProfileImageView.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), EditProfilePage.class));
        });

        seeMorePet.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ViewAllPetPage.class));
        });

        resetPassImageView.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), ResetPasswordPage.class));
        });

        return view;
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            emailTextView.setText(user.getEmail());
            String uid = user.getUid();

            db.collection(FirestoreCollection.USERS)
                    .document(uid).collection(FirestoreCollection.PROFILE)
                    .document(FirestoreCollection.USER_DETAILS)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String username = documentSnapshot.getString("username");
                            String base64Image = documentSnapshot.getString("profilePictureBase64");

                            usernameTextView.setText(username);

                            if (base64Image != null && !base64Image.isEmpty()) {
                                byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);

                                Glide.with(this)
                                        .asBitmap()
                                        .load(decodedBytes)
                                        .circleCrop()
                                        .into(profileImageView);
                            }
                        }
                    });
        }
    }

    private void loadUserPets() {
        backgroundColors = new int[]{
                ContextCompat.getColor(requireContext(), R.color.blue),
                ContextCompat.getColor(requireContext(), R.color.yellow),
                ContextCompat.getColor(requireContext(), R.color.pink),
                ContextCompat.getColor(requireContext(), R.color.green)
        };

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection(FirestoreCollection.USERS)
                    .document(user.getUid())
                    .collection(FirestoreCollection.PET)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        if (!querySnapshot.isEmpty()) {
                            noPetView.setVisibility(View.GONE);
                            petsContainer.setVisibility(View.VISIBLE);

                            petsContainer.removeAllViews();

                            for (QueryDocumentSnapshot documentSnapshot : querySnapshot) {
                                String petName = documentSnapshot.getString("petName");
                                String petType = documentSnapshot.getString("type");
                                String imageBase64 = documentSnapshot.getString("petPictureBase64");

                                View petView = LayoutInflater.from(getContext())
                                        .inflate(R.layout.pet_item, petsContainer, false);

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

                                petsContainer.addView(petView);
                            }
                        } else {
                            petsContainer.setVisibility(View.GONE);
                            noPetView.setVisibility(View.VISIBLE);
                        }
                    })
                    .addOnFailureListener(e -> {
                        noPetView.setVisibility(View.VISIBLE);
                    });
        }
    }


    private void showLogoutBottomSheet() {
        if (getContext() == null) return;

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View sheetView = LayoutInflater.from(getContext()).inflate(R.layout.logout_bottom_sheet, null);
        if (sheetView == null) return;

        bottomSheetDialog.setContentView(sheetView);

        TextView confirmLogout = sheetView.findViewById(R.id.btnLogout);
        TextView cancelLogout = sheetView.findViewById(R.id.stayHere);

        confirmLogout.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            if (mAuth != null) {
                mAuth.signOut();
            }

            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        cancelLogout.setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

}