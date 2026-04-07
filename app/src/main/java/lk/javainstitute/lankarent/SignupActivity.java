package lk.javainstitute.lankarent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import model.User;

public class SignupActivity extends AppCompatActivity {
    private EditText editText1, editText2, editText3, editText4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.yellow)); // Use your yellow color
        }


        editText1 = findViewById(R.id.editTextName);
        editText2 = findViewById(R.id.editTextPhone);
        editText3 = findViewById(R.id.editTextEmail);
        editText4 = findViewById(R.id.editTextPassword);
        Button signUpButton = findViewById(R.id.signUpButton);

        signUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String name = editText1.getText().toString();
                String phone = editText2.getText().toString();
                String email = editText3.getText().toString();
                String password = editText4.getText().toString();

                if (validateInputs(name, phone, email, password)) {
                    registerUser(name, phone, email, password);
                }
            }
        });

        Button loginScreenButton = findViewById(R.id.loginScreenButton);
        loginScreenButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(SignupActivity.this,LoginActivity.class);
                startActivity(intent);
            }
        });
    }

    private boolean validateInputs(String name, String phone, String email, String password) {
        if (name.isEmpty()) {
            showToast("Please fill in your name");
            return false;
        }

        if (phone.isEmpty()) {
            showToast("Please fill in your phone number");
            return false;
        } else if (!phone.matches("[0-9]+") || phone.length() < 10) {
            showToast("Enter a valid phone number");
            return false;
        }

        if (email.isEmpty()) {
            showToast("Please fill in your email address");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Invalid email address");
            return false;
        }

        if (password.isEmpty()) {
            showToast("Please fill in your password");
            return false;
        } else if (password.length() < 6) {
            showToast("Password must be at least 6 characters");
            return false;
        }

        return true;
    }

    private void showToast(String message) {
        Toast.makeText(SignupActivity.this, message, Toast.LENGTH_SHORT).show();
    }


private void registerUser(String name, String phone, String email, String password) {
    FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();

    firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        // Send email verification
                        sendVerificationEmail(firebaseUser);

                        // Get FCM token before saving user data
                        FirebaseMessaging.getInstance().getToken()
                                .addOnSuccessListener(token -> {
                                    saveUserData(firebaseUser.getUid(), name, phone, email, token);
                                })
                                .addOnFailureListener(e -> showToast("Failed to get FCM token: " + e.getMessage()));
                    }
                } else {
                    showToast("Registration failed: " + task.getException().getMessage());
                }
            });
}

    private void sendVerificationEmail(FirebaseUser firebaseUser) {
        firebaseUser.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showToast("Verification email sent. Please verify your email before logging in.");
                    } else {
                        showToast("Failed to send verification email: " + task.getException().getMessage());
                    }
                });
    }
    private void saveUserData(String userId, String name, String phone, String email,String fcmToken) {
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        sharedPreferences.edit().putBoolean("isSignedUp", true).apply();
        String role = sharedPreferences.getString("userRole", "landlord");

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        User user = new User(name, phone, email, role, fcmToken);

        firestore.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(unused -> {
                    showToast("User registered successfully!");

                    showToast("Signup successful! Please verify your email before logging in.");
                    startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Failed to save user data: " + e.getMessage()));
    }
}
