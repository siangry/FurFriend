package com.example.furfriend.screen.profile;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.furfriend.R;

import java.util.List;
import java.util.Random;

public class PetAdapter extends RecyclerView.Adapter<PetAdapter.PetViewHolder> {
    private Context context;
    private List<Pet> petList;
    private int[] backgroundColors;

    public PetAdapter(Context context, List<Pet> petList) {
        this.context = context;
        this.petList = petList;

        backgroundColors = new int[]{
                ContextCompat.getColor(context, R.color.blue),
                ContextCompat.getColor(context, R.color.yellow),
                ContextCompat.getColor(context, R.color.pink),
                ContextCompat.getColor(context, R.color.green)
        };
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.pet_detail_item, parent, false);
        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        Pet pet = petList.get(position);

        holder.textViewName.setText(pet.getName());
        holder.textViewType.setText(pet.getType());
        holder.textViewAge.setText(String.valueOf(pet.getAge()));
        holder.textViewAgeUnit.setText(pet.getAgeUnit());
        holder.textViewGender.setText(pet.getGender());

        String base64Image = pet.getPetImage();

        if (base64Image != null && !base64Image.isEmpty()) {
            byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);

            Glide.with(context)
                    .asBitmap()
                    .load(decodedBytes)
                    .circleCrop()
                    .into(holder.imageViewPet);
        } else {
            holder.imageViewPet.setImageResource(R.drawable.ic_animal_image);
        }

        Random random = new Random();
        int randomColor = backgroundColors[random.nextInt(backgroundColors.length)];
        GradientDrawable backgroundDrawable = new GradientDrawable();
        backgroundDrawable.setColor(randomColor);
        backgroundDrawable.setCornerRadius(100f);
        holder.itemView.setBackground(backgroundDrawable);
    }

    @Override
    public int getItemCount() {
        return petList.size();
    }

    public static class PetViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewPet, imageEditPet;
        TextView textViewName, textViewType, textViewAge, textViewAgeUnit, textViewGender;

        public PetViewHolder(@NonNull View itemView) {
            super(itemView);
            imageViewPet = itemView.findViewById(R.id.petImage);
            textViewName = itemView.findViewById(R.id.petName);
            textViewType = itemView.findViewById(R.id.petType);
            textViewAge = itemView.findViewById(R.id.petAge);
            textViewAgeUnit = itemView.findViewById(R.id.petAgeUnit);
            textViewGender = itemView.findViewById(R.id.petGender);
            imageEditPet = itemView.findViewById(R.id.editPet);
        }
    }
}
