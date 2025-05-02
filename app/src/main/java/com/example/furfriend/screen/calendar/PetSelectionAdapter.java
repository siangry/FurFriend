package com.example.furfriend.screen.calendar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.furfriend.R;

import java.util.ArrayList;
import java.util.List;

public class PetSelectionAdapter extends RecyclerView.Adapter<PetSelectionAdapter.PetViewHolder> {
    private List<PetItem> pets;
    private List<PetItem> selectedPets;
    private OnPetSelectionChangeListener selectionChangeListener;

    public interface OnPetSelectionChangeListener {
        void onSelectionChanged();
    }

    public PetSelectionAdapter(List<PetItem> pets) {
        this.pets = pets;
        this.selectedPets = new ArrayList<>();
    }

    public void setOnPetSelectionChangeListener(OnPetSelectionChangeListener listener) {
        this.selectionChangeListener = listener;
    }

    public void selectPet(String petId) {
        for (PetItem pet : pets) {
            if (pet.getId().equals(petId) && !selectedPets.contains(pet)) {
                selectedPets.add(pet);
                if (selectionChangeListener != null) {
                    selectionChangeListener.onSelectionChanged();
                }
                break;
            }
        }
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pet_selection, parent, false);
        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        PetItem pet = pets.get(position);
        holder.tvPetName.setText(pet.getName());
        holder.checkboxPet.setChecked(selectedPets.contains(pet));

        holder.checkboxPet.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedPets.contains(pet)) {
                    selectedPets.add(pet);
                }
            } else {
                selectedPets.remove(pet);
            }
            if (selectionChangeListener != null) {
                selectionChangeListener.onSelectionChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return pets.size();
    }

    public List<PetItem> getSelectedPets() {
        return selectedPets;
    }

    static class PetViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkboxPet;
        TextView tvPetName;

        PetViewHolder(View itemView) {
            super(itemView);
            checkboxPet = itemView.findViewById(R.id.checkboxPet);
            tvPetName = itemView.findViewById(R.id.tvPetName);
        }
    }

    public static class PetItem {
        private String id;
        private String name;

        public PetItem(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PetItem petItem = (PetItem) o;
            return id.equals(petItem.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }
} 