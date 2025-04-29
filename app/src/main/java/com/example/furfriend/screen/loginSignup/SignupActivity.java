package com.example.furfriend.screen.loginSignup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.furfriend.FirestoreCollection;
import com.example.furfriend.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SignupActivity extends AppCompatActivity {

    EditText username, email, password;
    Button signUpBtn;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        username = findViewById(R.id.usernameEditText);
        email = findViewById(R.id.emailEditText);
        password = findViewById(R.id.passwordEditText);
        signUpBtn = findViewById(R.id.btnSignup);

        TextWatcher watcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                String pass = password.getText().toString();
                String user = username.getText().toString();
                String mail = email.getText().toString();
                boolean valid = isValidPassword(pass) && !user.isEmpty() && !mail.isEmpty();

                signUpBtn.setEnabled(valid);
                signUpBtn.setBackgroundTintList(ContextCompat.getColorStateList(SignupActivity.this, valid ? R.color.orange : R.color.grey));
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        username.addTextChangedListener(watcher);
        email.addTextChangedListener(watcher);
        password.addTextChangedListener(watcher);

        signUpBtn.setOnClickListener(v -> {
            String user = username.getText().toString();
            String mail = email.getText().toString();
            String pass = password.getText().toString();

            checkUsernameExists(user, exists -> {
                if (exists) {
                    Toast.makeText(this, R.string.usernameExisted, Toast.LENGTH_SHORT).show();
                } else {
                    mAuth.createUserWithEmailAndPassword(mail, pass)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    String uid = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();
                                    Map<String, Object> userMap = new HashMap<>();
                                    userMap.put("username", user);
                                    userMap.put("email", mail);

                                    db.collection(FirestoreCollection.USERS).document(uid).collection(FirestoreCollection.PROFILE).document(FirestoreCollection.USER_DETAILS).set(userMap).addOnSuccessListener(unused -> {
                                        Toast.makeText(this, R.string.accountCreatedSuccessfully, Toast.LENGTH_SHORT).show();
                                        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                                        startActivity(intent);
                                        finish();
                                    });
                                } else {
                                    Exception exception = task.getException();
                                    if (exception instanceof FirebaseAuthUserCollisionException) {
                                        Toast.makeText(SignupActivity.this, R.string.emailExisted, Toast.LENGTH_SHORT).show();
                                    } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                                        Toast.makeText(SignupActivity.this, R.string.incorrectEmailFormat, Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(SignupActivity.this, R.string.signupFailed, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                }
            });
        });

        findViewById(R.id.goToLogin).setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
        });
    }

    private boolean isValidPassword(String password) {
        return password.length() >= 6 &&
                password.matches(".*[A-Z].*") &&
                password.matches(".*[a-z].*") &&
                password.matches(".*\\d.*") &&
                password.matches(".*[^a-zA-Z0-9].*");
    }

    private void checkUsernameExists(String username, OnUsernameCheckListener listener) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(FirestoreCollection.USERS)
                .whereEqualTo("username", username)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean exists = !task.getResult().isEmpty();
                        listener.onCheck(exists);
                    } else {
                        listener.onCheck(false);
                    }
                });
    }

    interface OnUsernameCheckListener {
        void onCheck(boolean exists);
    }
}