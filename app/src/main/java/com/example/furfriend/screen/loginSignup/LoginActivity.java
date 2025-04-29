package com.example.furfriend.screen.loginSignup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.furfriend.MainActivity;
import com.example.furfriend.R;
import com.example.furfriend.screen.profile.ResetPasswordPage;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {
    private EditText email, password;
    private TextView goSignUp;
    private Button loginBtn;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        email = findViewById(R.id.emailEditText);
        password = findViewById(R.id.passwordEditText);
        loginBtn = findViewById(R.id.btnLogin);
        goSignUp = findViewById(R.id.goToSignUp);

        TextWatcher watcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                boolean enable = !email.getText().toString().isEmpty() && !password.getText().toString().isEmpty();
                loginBtn.setEnabled(enable);
                loginBtn.setBackgroundTintList(ContextCompat.getColorStateList(LoginActivity.this, enable ? R.color.orange : R.color.grey));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        email.addTextChangedListener(watcher);
        password.addTextChangedListener(watcher);

        loginBtn.setOnClickListener(v -> {
            mAuth.signInWithEmailAndPassword(email.getText().toString(), password.getText().toString())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            Toast.makeText(this, R.string.loginFailed, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        goSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });
    }
}