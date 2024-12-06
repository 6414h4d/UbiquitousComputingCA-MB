package com.example.microbitca;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText registerUsernameInput, registerPasswordInput, emailInput;
    private Button registerButton, backToLoginButton;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        registerUsernameInput = findViewById(R.id.registerUsername);
        registerPasswordInput = findViewById(R.id.registerPassword);
        emailInput = findViewById(R.id.emailInput);
        registerButton = findViewById(R.id.registerButton);
        backToLoginButton = findViewById(R.id.backToLoginButton);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateAndRegister();
            }
        });

        // Handle back to login button click
        backToLoginButton.setOnClickListener(v -> {
            // Navigate to MainActivity (Login Activity)
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void validateAndRegister() {
        String username = registerUsernameInput.getText().toString().trim();
        String password = registerPasswordInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(username)) {
            registerUsernameInput.setError("Username is required");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            registerPasswordInput.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            registerPasswordInput.setError("Password must be at least 6 characters");
            return;
        }

        if (TextUtils.isEmpty(email) || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email address");
            return;
        }

        // Firebase Authentication for user registration
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Registration successful
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            saveUserDataToDatabase(firebaseUser.getUid(), username, email);
                        }
                    } else {
                        // Registration failed
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserDataToDatabase(String userId, String username, String email) {
        // Save user data to Firestore
        Map<String, Object> user = new HashMap<>();
        user.put("username", username);
        user.put("email", email);

        db.collection("users")
                .document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();

                    // Redirect to Login Activity (MainActivity)
                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Failed to save user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}