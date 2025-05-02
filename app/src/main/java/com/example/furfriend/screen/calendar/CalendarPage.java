package com.example.furfriend.screen.calendar;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.furfriend.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class CalendarPage extends Fragment implements ActivityAdapter.OnActivityClickListener, ReminderAdapter.OnReminderClickListener {
    private RecyclerView activitiesRv;
    private RecyclerView remindersRv;
    private TextView emptyActivitiesText;
    private TextView emptyRemindersText;
    private ActivityAdapter activityAdapter;
    private ReminderAdapter reminderAdapter;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.screen_calendar_page, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        activitiesRv = view.findViewById(R.id.activitiesRecyclerView);
        remindersRv = view.findViewById(R.id.remindersRecyclerView);
        emptyActivitiesText = view.findViewById(R.id.emptyActivitiesText);
        emptyRemindersText = view.findViewById(R.id.emptyRemindersText);

        setupRecyclerViews();
        fetchActivities();
        fetchReminders();

        return view;
    }

    private void setupRecyclerViews() {
        // Setup Activities RecyclerView
        activitiesRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        activityAdapter = new ActivityAdapter(new ArrayList<>(), this);
        activitiesRv.setAdapter(activityAdapter);

        // Setup Reminders RecyclerView
        remindersRv.setLayoutManager(new LinearLayoutManager(requireContext()));
        reminderAdapter = new ReminderAdapter(new ArrayList<>(), this);
        remindersRv.setAdapter(reminderAdapter);
    }

    private void fetchActivities() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("activities")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Map<String, Object>> activities = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Map<String, Object> activity = document.getData();
                    activity.put("id", document.getId());
                    activities.add(activity);
                }

                // Update UI based on whether there are activities
                if (activities.isEmpty()) {
                    emptyActivitiesText.setVisibility(View.VISIBLE);
                    activitiesRv.setVisibility(View.GONE);
                } else {
                    emptyActivitiesText.setVisibility(View.GONE);
                    activitiesRv.setVisibility(View.VISIBLE);
                    activityAdapter.updateActivities(activities);
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(requireContext(), "Error loading activities: " + e.getMessage(),
                             Toast.LENGTH_SHORT).show();
            });
    }

    private void fetchReminders() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("reminders")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Map<String, Object>> reminders = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Map<String, Object> reminder = document.getData();
                    reminder.put("id", document.getId());
                    reminders.add(reminder);
                }

                // Update UI based on whether there are reminders
                if (reminders.isEmpty()) {
                    emptyRemindersText.setVisibility(View.VISIBLE);
                    remindersRv.setVisibility(View.GONE);
                } else {
                    emptyRemindersText.setVisibility(View.GONE);
                    remindersRv.setVisibility(View.VISIBLE);
                    reminderAdapter.updateReminders(reminders);
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(requireContext(), "Error loading reminders: " + e.getMessage(),
                             Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Add click listener for activities right arrow
        ImageView activitiesIcon = view.findViewById(R.id.activitiesIcon);
        activitiesIcon.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), AllActivitiesPage.class);
            startActivity(intent);
        });

        // Add click listener for reminders right arrow
        ImageView remindersIcon = view.findViewById(R.id.remindersIcon);
        remindersIcon.setOnClickListener(v -> {
            Intent intent = new Intent(requireActivity(), AllRemindersPage.class);
            startActivity(intent);
        });
    }

    @Override
    public void onActivityClick(Map<String, Object> activity) {
        // Open AllActivitiesPage when an activity is clicked
        Intent intent = new Intent(requireActivity(), AllActivitiesPage.class);
        startActivity(intent);
    }

    @Override
    public void onReminderClick(Map<String, Object> reminder) {
        // Open AllRemindersPage when a reminder is clicked
        Intent intent = new Intent(requireActivity(), AllRemindersPage.class);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchActivities(); // Refresh activities when returning to this fragment
        fetchReminders(); // Refresh reminders when returning to this fragment
    }
}