package com.example.furfriend.onboarding;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.example.furfriend.R;
import com.example.furfriend.screen.loginSignup.LoginActivity;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {
    ViewPager2 viewPager;
    OnboardingAdapter adapter;
    Button btnNext;
    int currentPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("onboarding", MODE_PRIVATE);
        if (prefs.getBoolean("completed", false)) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_onboarding);

        viewPager = findViewById(R.id.viewPager);
        btnNext = findViewById(R.id.btnNext);

        List<OnboardingItem> items = new ArrayList<>();
        items.add(new OnboardingItem(R.drawable.ic_onboarding_1, R.drawable.ic_indicator_1, getString(R.string.onboardingTitle1), getString(R.string.onboardingDesc1)));
        items.add(new OnboardingItem(R.drawable.ic_onboarding_2, R.drawable.ic_indicator_2, getString(R.string.onboardingTitle2), getString(R.string.onboardingDesc2)));
        items.add(new OnboardingItem(R.drawable.ic_onboarding_3, R.drawable.ic_indicator_3, getString(R.string.onboardingTitle3), getString(R.string.onboardingDesc3)));

        adapter = new OnboardingAdapter(items);
        viewPager.setAdapter(adapter);

        btnNext.setOnClickListener(v -> {
            if (currentPosition < items.size() - 1) {
                currentPosition++;
                viewPager.setCurrentItem(currentPosition);
            } else {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("completed", true);
                editor.apply();

                startActivity(new Intent(OnboardingActivity.this, LoginActivity.class));
                finish();
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPosition = position;
                btnNext.setText(position == items.size() - 1 ? getString(R.string.getStarted) : getString(R.string.next));
            }
        });
    }
}
