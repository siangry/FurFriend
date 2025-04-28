package com.example.furfriend.screen.profile;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import com.example.furfriend.R;

public class UpdateSuccessPage extends AppCompatActivity {

    private Button buttonGoToProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_success_page);

        buttonGoToProfile = findViewById(R.id.btnDone);

        buttonGoToProfile.setOnClickListener(v -> {
            ProfilePage profileFragment = new ProfilePage();

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(android.R.id.content, profileFragment);
            transaction.addToBackStack(null);
            transaction.commit();

            finish();
        });
    }
}