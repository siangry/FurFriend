package com.example.furfriend.screen.calendar;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.example.furfriend.R;

public class Success extends AppCompatActivity {

    private TextView tvSuccessMessage;
    private Button btnDone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        tvSuccessMessage = findViewById(R.id.tvSuccessMessage);
        btnDone = findViewById(R.id.btnDone);

        // Get the type of item added (activity or reminder)
        String type = getIntent().getStringExtra("TYPE");
        if (type != null && type.equals("reminder")) {
            tvSuccessMessage.setText("A reminder has been added successfully!");
        } else {
            tvSuccessMessage.setText("An activity has been added successfully!");
        }

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Simply finish this activity to go back to the previous screen
                finish();
            }
        });
    }
}