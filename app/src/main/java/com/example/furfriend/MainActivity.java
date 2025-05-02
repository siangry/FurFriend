package com.example.furfriend;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

import com.example.furfriend.databinding.ActivityMainBinding;
import com.example.furfriend.screen.calendar.CalendarPage;
import com.example.furfriend.screen.home.HomePage;
import com.example.furfriend.screen.profile.ProfilePage;
import com.example.furfriend.screen.search.SearchPage;
import com.example.furfriend.screen.social.SocialPage;


public class MainActivity extends BaseActivity {
    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        replaceFragment(new HomePage());

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_home) {
                replaceFragment(new HomePage());
            } else if (item.getItemId() == R.id.nav_search) {
                replaceFragment(new SearchPage());
            } else if (item.getItemId() == R.id.nav_social) {
                replaceFragment(new SocialPage());
            } else if (item.getItemId() == R.id.nav_calendar) {
                replaceFragment(new CalendarPage());
            } else if (item.getItemId() == R.id.nav_profile) {
                replaceFragment(new ProfilePage());
            }
            return true;
        });
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }
}