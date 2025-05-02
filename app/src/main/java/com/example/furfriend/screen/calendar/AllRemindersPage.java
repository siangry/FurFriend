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

public class AllRemindersPage extends AppCompatActivity implements AllRemindersAdapter.OnReminderActionListener {
    private RecyclerView remindersRv;
    private TextView emptyRemindersText;
    private ImageView btnBack;
    private ImageButton fabMenu;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private AllRemindersAdapter reminderAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_reminders_page);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        remindersRv = findViewById(R.id.remindersRecyclerView);
        emptyRemindersText = findViewById(R.id.emptyRemindersText);
        btnBack = findViewById(R.id.btnBack);
        fabMenu = findViewById(R.id.fabMenu);

        remindersRv.setLayoutManager(new LinearLayoutManager(this));
        reminderAdapter = new AllRemindersAdapter(new ArrayList<>(), this);
        remindersRv.setAdapter(reminderAdapter);

        btnBack.setOnClickListener(v -> finish());
        setupFabMenu();

        fetchReminders();
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
                emptyRemindersText.setVisibility(View.VISIBLE);
                remindersRv.setVisibility(View.GONE);
            });
    }

    @Override
    public void onEditReminder(Map<String, Object> reminder) {
        Intent intent = new Intent(this, EditReminderPage.class);
        intent.putExtra("reminderId", (String) reminder.get("id"));
        intent.putExtra("title", (String) reminder.get("title"));
        intent.putExtra("description", (String) reminder.get("description"));
        intent.putExtra("date", (String) reminder.get("date"));
        intent.putExtra("time", (String) reminder.get("time"));
        startActivity(intent);
    }

    @Override
    public void onDeleteReminder(String reminderId) {
        db.collection("reminders")
            .document(reminderId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Reminder deleted successfully", Toast.LENGTH_SHORT).show();
                fetchReminders(); // Refresh the list
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error deleting reminder: " + e.getMessage(), 
                             Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchReminders(); // Refresh reminders when returning to this activity
    }
} 