package com.example.furfriend.onboarding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.furfriend.R;

import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private List<OnboardingItem> onboardingItems;

    public OnboardingAdapter(List<OnboardingItem> onboardingItems) {
        this.onboardingItems = onboardingItems;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.onboarding_item, parent, false);
        return new OnboardingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.bind(onboardingItems.get(position));
    }

    @Override
    public int getItemCount() {
        return onboardingItems.size();
    }

    class OnboardingViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        ImageView indicator;
        TextView title, desc;
        Button btn;

        OnboardingViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.onboardingImage);
            indicator = itemView.findViewById(R.id.onboardingIndicator);
            title = itemView.findViewById(R.id.onboardingTitle);
            desc = itemView.findViewById(R.id.onboardingDesc);
            btn = itemView.findViewById(R.id.btnNext);
        }

        void bind(OnboardingItem item) {
            image.setImageResource(item.imageId);
            indicator.setImageResource(item.indicatorId);
            title.setText(item.title);
            desc.setText(item.description);
        }
    }
}
