package com.example.furfriend.screen.calendar;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.furfriend.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AllActivitiesPage extends AppCompatActivity implements AllActivitiesAdapter.OnActivityActionListener {
    private RecyclerView activitiesRv;
    private TextView emptyActivitiesText;
    private ImageView btnBack;
    private ImageButton fabMenu;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private AllActivitiesAdapter activityAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_activities_page);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        activitiesRv = findViewById(R.id.activitiesRecyclerView);
        emptyActivitiesText = findViewById(R.id.emptyActivitiesText);
        btnBack = findViewById(R.id.btnBack);
        fabMenu = findViewById(R.id.fabMenu);

        activitiesRv.setLayoutManager(new LinearLayoutManager(this));
        activityAdapter = new AllActivitiesAdapter(new ArrayList<>(), this);
        activitiesRv.setAdapter(activityAdapter);

        btnBack.setOnClickListener(v -> finish());
        setupFabMenu();

        fetchActivities();
    }

    private void setupFabMenu() {
        fabMenu.setOnClickListener(view -> {
            View popupView = LayoutInflater.from(this)
                    .inflate(R.layout.popup_menu, null);

            popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
            int popupHeight = popupView.getMeasuredHeight();

            PopupWindow popupWindow = new PopupWindow(
                    popupView,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    true);

            popupWindow.setAnimationStyle(R.style.PopupAnimation);
            popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                popupWindow.setElevation(16f);
            }

            int[] fabLocation = new int[2];
            fabMenu.getLocationOnScreen(fabLocation);
            int fabX = fabLocation[0];
            int fabY = fabLocation[1];

            int xOffset = 0;
            int yOffset = -popupHeight;

            popupWindow.showAtLocation(
                    fabMenu,
                    Gravity.NO_GRAVITY,
                    fabX + xOffset,
                    fabY + yOffset);

            setupPopupButtons(popupView, popupWindow);
        });
    }

    private void setupPopupButtons(View popupView, PopupWindow popupWindow) {
        ImageView downloadButton = popupView.findViewById(R.id.btn_download);
        ImageView reminderButton = popupView.findViewById(R.id.btn_reminder);
        ImageView activityButton = popupView.findViewById(R.id.btn_activity);

        downloadButton.setOnClickListener(v -> {
            popupWindow.dismiss();
            Intent intent = new Intent(this, ExportPdfPage.class);
            startActivity(intent);
        });

        reminderButton.setOnClickListener(v -> {
            popupWindow.dismiss();
            Intent intent = new Intent(this, AddReminder.class);
            startActivity(intent);
        });

        activityButton.setOnClickListener(v -> {
            popupWindow.dismiss();
            Intent intent = new Intent(this, AddActivity.class);
            startActivity(intent);
        });
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
                emptyActivitiesText.setVisibility(View.VISIBLE);
                activitiesRv.setVisibility(View.GONE);
            });
    }

    @Override
    public void onEditActivity(Map<String, Object> activity) {
        Intent intent = new Intent(this, EditActivityPage.class);
        intent.putExtra("activityId", (String) activity.get("id"));
        intent.putExtra("title", (String) activity.get("title"));
        intent.putExtra("description", (String) activity.get("description"));
        intent.putExtra("date", (String) activity.get("date"));
        intent.putExtra("time", (String) activity.get("time"));
        startActivity(intent);
    }

    @Override
    public void onDeleteActivity(String activityId) {
        db.collection("activities")
            .document(activityId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Activity deleted successfully", Toast.LENGTH_SHORT).show();
                fetchActivities(); // Refresh the list
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error deleting activity: " + e.getMessage(), 
                             Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchActivities(); // Refresh activities when returning to this activity
    }
} 