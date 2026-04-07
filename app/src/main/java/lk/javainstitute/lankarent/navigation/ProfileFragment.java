package lk.javainstitute.lankarent.navigation;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import lk.javainstitute.lankarent.LandlordDashboardActivity;
import lk.javainstitute.lankarent.R;
import lk.javainstitute.lankarent.TenantDashboardActivity;

public class ProfileFragment extends Fragment {

    private EditText nameEditText, phoneEditText ;

    private TextView email_Text, user_role ,letter;
    private FirebaseFirestore firestore;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Initialize views
        nameEditText = view.findViewById(R.id.nameEdit_Text);
        phoneEditText = view.findViewById(R.id.phoneEdit_Text);
        email_Text = view.findViewById(R.id.email_Text);
        user_role = view.findViewById(R.id.textView30);
        letter = view.findViewById(R.id.letterText);
        Button updateButton = view.findViewById(R.id.button);
        ImageView backButton = view.findViewById(R.id.backButton);


        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUser = firebaseAuth.getCurrentUser();

        // Load user details if the user is logged in
        if (currentUser != null) {
            loadUserDetails(currentUser.getUid());
        }

        // Set click listener for the update button
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUserDetails();
            }
        });


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

    private void loadUserDetails(String userId) {
        firestore.collection("users").document(userId)
                .get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot documentSnapshot = task.getResult();
                            if (documentSnapshot.exists()) {
                                String userName = documentSnapshot.getString("name");
                                nameEditText.setText(userName);
                                phoneEditText.setText(documentSnapshot.getString("phone"));
                                email_Text.setText(documentSnapshot.getString("email"));
                                user_role.setText(documentSnapshot.getString("role"));

                                // Set the first letter of the user's name to letterText
                                if (userName != null && !userName.isEmpty()) {
                                    char firstLetter = userName.charAt(0);
                                    letter.setText(String.valueOf(Character.toUpperCase(firstLetter)));

                                    // Ensure UI updates on the landlord_fragment_container thread
                                    letter.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            letter.requestLayout();
                                        }
                                    });
                                }
                            } else {
                                Toast.makeText(getContext(), "User data not found", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getContext(), "Failed to load user data", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    private void updateUserDetails() {
        String newName = nameEditText.getText().toString().trim();
        String newPhone = phoneEditText.getText().toString().trim();

        // Validate name field
        if (newName.isEmpty()) {
            nameEditText.setError("Name cannot be empty");
            nameEditText.requestFocus();
            return;
        }

        // Validate phone number format
        if (!newPhone.matches("^[0-9]{10}$")) {
            phoneEditText.setError("Enter a valid 10-digit Sri Lankan phone number");
            phoneEditText.requestFocus();
            return;
        }

        if (currentUser != null) {
            firestore.collection("users").document(currentUser.getUid())
                    .update("name", newName, "phone", newPhone)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Toast.makeText(getContext(), "User details updated successfully", Toast.LENGTH_SHORT).show();

                            // Update the letter dynamically
                            char firstLetter = Character.toUpperCase(newName.charAt(0));
                            letter.setText(String.valueOf(firstLetter));

                            // Force UI update
                            letter.post(new Runnable() {
                                @Override
                                public void run() {
                                    letter.requestLayout();
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getContext(), "Failed to update user details", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

}