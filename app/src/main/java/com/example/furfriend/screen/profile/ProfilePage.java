package com.example.furfriend.screen.profile;

import androidx.fragment.app.Fragment;

import android.content.Intent;
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

public class ProfilePage extends Fragment {
    private ImageView profileImageView, editProfileImageView;
    private TextView usernameTextView, emailTextView,logoutTextView;
    private LinearLayout petsContainer, noPetView,  seeMorePet;
    private Button addPetButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

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
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection(FirestoreCollection.USERS)
                    .document(user.getUid())
                    .collection(FirestoreCollection.PROFILE)
                    .document(FirestoreCollection.PET)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            noPetView.setVisibility(View.GONE);

                            String petName = documentSnapshot.getString("petName");
                            String petType = documentSnapshot.getString("type");

                            View petView = LayoutInflater.from(getContext())
                                    .inflate(android.R.layout.simple_list_item_2, petsContainer, false);
                            TextView text1 = petView.findViewById(android.R.id.text1);
                            TextView text2 = petView.findViewById(android.R.id.text2);

                            text1.setText(petName != null ? petName : "Unknown Name");
                            text2.setText(petType != null ? petType : "Unknown Type");

                            petsContainer.addView(petView);
                        } else {
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