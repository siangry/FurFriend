package com.example.furfriend.screen.calendar;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.furfriend.Constants;
import com.example.furfriend.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class AllActivitiesAdapter extends RecyclerView.Adapter<AllActivitiesAdapter.ActivityViewHolder> {
    private List<Map<String, Object>> activities;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private OnActivityActionListener listener;

    public interface OnActivityActionListener {
        void onEditActivity(Map<String, Object> activity);
        void onDeleteActivity(String activityId);
    }

    public AllActivitiesAdapter(List<Map<String, Object>> activities, OnActivityActionListener listener) {
        this.activities = activities;
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ActivityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_activity_with_description, parent, false);
        return new ActivityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ActivityViewHolder holder, int position) {
        Map<String, Object> activity = activities.get(position);
        String activityId = (String) activity.get("id");
        
        // Set background color based on position
        int[] colors = {R.color.yellow, R.color.blue, R.color.pink, R.color.green};
        holder.cardBackground.setBackgroundResource(colors[position % colors.length]);
        
        holder.activityTitle.setText((String) activity.get("title"));
        holder.activityDescription.setText((String) activity.get("description"));
        String date = (String) activity.get("date");
        String time = (String) activity.get("time");
        holder.activityDateTime.setText(date + ", " + time);

        // Set click listeners for edit and delete buttons
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditActivity(activity);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                .setTitle("Delete Activity")
                .setMessage("Are you sure you want to delete this activity?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (listener != null) {
                        listener.onDeleteActivity(activityId);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
        });

        // Set pet names
        List<Map<String, String>> pets = (List<Map<String, String>>) activity.get("pets");
        if (pets != null && !pets.isEmpty()) {
            StringBuilder petNamesBuilder = new StringBuilder();
            for (int i = 0; i < pets.size(); i++) {
                if (i > 0) {
                    petNamesBuilder.append(", ");
                }
                petNamesBuilder.append(pets.get(i).get("petName"));
            }
            holder.petNames.setText(petNamesBuilder.toString());

            // Show up to 3 pet images
            ImageView[] petImages = {holder.petImage1, holder.petImage2, holder.petImage3};
            for (int i = 0; i < 3; i++) {
                if (i < pets.size()) {
                    String petId = pets.get(i).get("petId");
                    String userId = mAuth.getCurrentUser().getUid();
                    ImageView imageView = petImages[i];
                    imageView.setVisibility(View.INVISIBLE);
                    if (petId != null && userId != null) {
                        db.collection(Constants.USERS)
                                .document(userId)
                                .collection(Constants.PET)
                                .document(petId)
                                .get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        String petImage = documentSnapshot.getString("petPictureBase64");
                                        if (petImage != null && !petImage.isEmpty()) {
                                            try {
                                                byte[] decodedString = Base64.decode(petImage, Base64.DEFAULT);
                                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                                                if (decodedByte != null) {
                                                    Glide.with(imageView.getContext())
                                                        .load(decodedByte != null ? decodedByte : R.drawable.ic_animal_image)
                                                        .circleCrop()
                                                        .error(R.drawable.ic_animal_image)
                                                        .placeholder(R.drawable.ic_animal_image)
                                                        .into(imageView);
                                                    imageView.setVisibility(View.VISIBLE);
                                                }
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                                imageView.setVisibility(View.GONE);
                                            }
                                        } else {
                                            Glide.with(imageView.getContext())
                                                .load(R.drawable.ic_animal_image)
                                                .circleCrop()
                                                .into(imageView);
                                            imageView.setVisibility(View.VISIBLE);
                                        }
                                    } else {
                                        imageView.setVisibility(View.GONE);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    e.printStackTrace();
                                    imageView.setVisibility(View.GONE);
                                });
                    } else {
                        imageView.setVisibility(View.GONE);
                    }
                } else {
                    petImages[i].setVisibility(View.GONE);
                }
            }
        } else {
            holder.petNames.setText("");
            holder.petImage1.setVisibility(View.GONE);
            holder.petImage2.setVisibility(View.GONE);
            holder.petImage3.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return activities.size();
    }

    public void updateActivities(List<Map<String, Object>> newActivities) {
        this.activities = newActivities;
        notifyDataSetChanged();
    }

    static class ActivityViewHolder extends RecyclerView.ViewHolder {
        TextView activityTitle;
        TextView activityDescription;
        TextView petNames;
        TextView activityDateTime;
        ImageView petImage1, petImage2, petImage3;
        LinearLayout petImagesContainer;
        RelativeLayout cardBackground;
        ImageButton btnEdit, btnDelete;

        ActivityViewHolder(View itemView) {
            super(itemView);
            activityTitle = itemView.findViewById(R.id.activityTitle);
            activityDescription = itemView.findViewById(R.id.activityDescription);
            petNames = itemView.findViewById(R.id.petNames);
            activityDateTime = itemView.findViewById(R.id.activityDateTime);
            petImage1 = itemView.findViewById(R.id.petImage1);
            petImage2 = itemView.findViewById(R.id.petImage2);
            petImage3 = itemView.findViewById(R.id.petImage3);
            petImagesContainer = itemView.findViewById(R.id.petImagesContainer);
            cardBackground = itemView.findViewById(R.id.cardBackground);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
} 