package com.example.furfriend.screen.profile;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.furfriend.BaseActivity;
import com.example.furfriend.R;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ResetPasswordPage extends BaseActivity {

    private ImageView btnBack;
    private EditText etCurrentPassword, etNewPassword, etConfirmPassword;
    private Button btnChangePassword;
    private boolean isLoginForgotPassword = false;

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password_page);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        btnBack = findViewById(R.id.btnBack);
        etCurrentPassword = findViewById(R.id.currentPassword);
        etNewPassword = findViewById(R.id.newPassword);
        etConfirmPassword = findViewById(R.id.confirmPassword);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        TextWatcher watcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                validateInputs();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };

        etCurrentPassword.addTextChangedListener(watcher);
        etNewPassword.addTextChangedListener(watcher);
        etConfirmPassword.addTextChangedListener(watcher);

        btnChangePassword.setOnClickListener(v -> {
            String currentPass = etCurrentPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            reauthenticateAndChangePassword(currentPass, newPass);
        });

        btnBack.setOnClickListener(v -> {
            getOnBackPressedDispatcher().onBackPressed();
        });
    }

    private void validateInputs() {
        String currentPass = etCurrentPassword.getText().toString().trim();
        String newPass = etNewPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        boolean valid = !currentPass.isEmpty()
                && isValidPassword(newPass)
                && newPass.equals(confirmPass);

        btnChangePassword.setEnabled(valid);
        btnChangePassword.setBackgroundTintList(ContextCompat.getColorStateList(ResetPasswordPage.this, valid ? R.color.orange : R.color.grey));
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 6 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*\\d.*") &&
                password.matches(".*[^a-zA-Z0-9].*");
    }

    private void reauthenticateAndChangePassword(String currentPass, String newPass) {
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(currentUser.getEmail(), currentPass);
        currentUser.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        currentUser.updatePassword(newPass)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        Toast.makeText(ResetPasswordPage.this, getString(R.string.passwordChangeSuccess), Toast.LENGTH_SHORT).show();
                                        finish(); // or navigate to another screen
                                    } else {
                                        Toast.makeText(ResetPasswordPage.this, getString(R.string.passwordUpdateFail), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(ResetPasswordPage.this, getString(R.string.currentPassIncorrect), Toast.LENGTH_SHORT).show();
                    }
                });
    }

}