package lk.javainstitute.lankarent.navigation;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.javainstitute.lankarent.LandlordDashboardActivity;
import lk.javainstitute.lankarent.R;
import lk.javainstitute.lankarent.TenantDashboardActivity;

public class SecurityFragment extends Fragment {

    private EditText currentPasswordEditText, newEmailEditText, newPassowordEditText, confirmPassowrdEditText;
    private FirebaseUser user;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_security, container, false);

        // Initialize FirebaseAuth
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();

        // Initialize UI elements
        currentPasswordEditText = view.findViewById(R.id.currentPasswordEditText);
        newEmailEditText = view.findViewById(R.id.newEmailEditText);
        newPassowordEditText = view.findViewById(R.id.newPassowordEditText);
        confirmPassowrdEditText = view.findViewById(R.id.confirmPassowrdEditText);
        Button updateButton = view.findViewById(R.id.button2);
        ImageView backButton = view.findViewById(R.id.backButton2);

        updateButton.setOnClickListener(v -> updateUserCredentials());

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getActivity() instanceof TenantDashboardActivity) {
                    Intent intent = new Intent(getActivity(), TenantDashboardActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                } else if (getActivity() instanceof LandlordDashboardActivity) {
                    Intent intent = new Intent(getActivity(), LandlordDashboardActivity.class);
                    startActivity(intent);
                    getActivity().finish();
                }
            }
        });

        return view;
    }

    private void updateUserCredentials() {
        String currentPassword = currentPasswordEditText.getText().toString().trim();
        String newEmail = newEmailEditText.getText().toString().trim();
        String newPassword = newPassowordEditText.getText().toString().trim();
        String confirmPassword = confirmPassowrdEditText.getText().toString().trim();

        if (TextUtils.isEmpty(currentPassword)) {
            currentPasswordEditText.setError("Enter current password");
            currentPasswordEditText.requestFocus();
            return;
        }

        // Authenticate user before making changes
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Validate email if entered
                if (!TextUtils.isEmpty(newEmail)) {
                    if (!isValidEmail(newEmail)) {
                        newEmailEditText.setError("Enter a valid email address");
                        newEmailEditText.requestFocus();
                        return;
                    }
                    if (newEmail.equals(user.getEmail())) {
                        newEmailEditText.setError("New email cannot be the same as current email");
                        newEmailEditText.requestFocus();
                        return;
                    }
                    updateEmail(newEmail);
                }

                // Validate password if entered
                if (!TextUtils.isEmpty(newPassword) || !TextUtils.isEmpty(confirmPassword)) {
                    if (!newPassword.equals(confirmPassword)) {
                        confirmPassowrdEditText.setError("Passwords do not match");
                        confirmPassowrdEditText.requestFocus();
                        return;
                    }
                    if (!isValidPassword(newPassword)) {
                        newPassowordEditText.setError("Password must be at least 6 characters");
                        newPassowordEditText.requestFocus();
                        return;
                    }
                    updatePassword(newPassword);
                }
            } else {
                Toast.makeText(getActivity(), "Authentication failed. Check current password.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Validate email format
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    // Validate password strength
    private boolean isValidPassword(String password) {
        String passwordPattern = "^.{6,}$";
        return password.matches(passwordPattern);
    }

    private void updateEmail(String newEmail) {
        user.verifyBeforeUpdateEmail(newEmail).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Update FireStore after email is successfully updated
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("users").document(user.getUid())
                        .update("email", newEmail)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(getActivity(), "Email updated successfully in Firestore!", Toast.LENGTH_SHORT).show()
                        )
                        .addOnFailureListener(e ->
                                Toast.makeText(getActivity(), "Failed to update email in Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                        );

                Toast.makeText(getActivity(), "Verification email sent. Check your inbox.", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), "Failed to update email: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePassword(String newPassword) {
        user.updatePassword(newPassword).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getActivity(), "Password updated successfully!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getActivity(), "Failed to update password: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
