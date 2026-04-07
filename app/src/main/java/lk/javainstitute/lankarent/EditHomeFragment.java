package lk.javainstitute.lankarent;

import android.app.AlertDialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class EditHomeFragment extends Fragment {

    private EditText titleEditText, addressEditText, rentAmountEditText;
    private String propertyId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_edit_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize views
        titleEditText = view.findViewById(R.id.propertyTitle);
        addressEditText = view.findViewById(R.id.propertyAddress);
        rentAmountEditText = view.findViewById(R.id.rentAmount);

        // Handle back button click
        ImageButton backButton = view.findViewById(R.id.imageButton1);
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Retrieve data from the bundle
        Bundle bundle = getArguments();
        if (bundle != null) {
            propertyId = bundle.getString("propertyId", "");
            String title = bundle.getString("title", "");
            String address = bundle.getString("address", "");
            double rentAmount = bundle.getDouble("rentAmount", 0.0);

            // Populate the fields with the retrieved data
            titleEditText.setText(title);
            addressEditText.setText(address);
            rentAmountEditText.setText(String.valueOf(rentAmount));

            // Handle save button click
            Button saveButton = view.findViewById(R.id.btnUpdateProperty);
            saveButton.setOnClickListener(v -> {
                // Retrieve updated values from the fields
                String updatedTitle = titleEditText.getText().toString().trim();
                String updatedAddress = addressEditText.getText().toString().trim();
                String rentAmountStr = rentAmountEditText.getText().toString().trim();

                // Validate fields
                if (isValidInput(updatedTitle, updatedAddress, rentAmountStr)) {
                    double updatedRentAmount = Double.parseDouble(rentAmountStr);

                    // Update the Firestore document
                    updateFirestoreDocument(propertyId, updatedTitle, updatedAddress, updatedRentAmount);
                }
            });

            Button deleteHome = view.findViewById(R.id.btnDeleteProperty);
            deleteHome.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    deleteFirestoreDocument(propertyId);
                }
            });

        } else {
            Toast.makeText(getContext(), "No data received", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidInput(String title, String address, String rentAmountStr) {
        // Check if title is empty
        if (title.isEmpty()) {
            titleEditText.setError("Title is required");
            titleEditText.requestFocus();
            return false;
        }

        // Check if address is empty
        if (address.isEmpty()) {
            addressEditText.setError("Address is required");
            addressEditText.requestFocus();
            return false;
        }

        // Check if rent amount is empty
        if (rentAmountStr.isEmpty()) {
            rentAmountEditText.setError("Rent amount is required");
            rentAmountEditText.requestFocus();
            return false;
        }

        // Check if rent amount is a valid number
        try {
            double rentAmount = Double.parseDouble(rentAmountStr);
            if (rentAmount <= 0) {
                rentAmountEditText.setError("Rent amount must be greater than 0");
                rentAmountEditText.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            rentAmountEditText.setError("Invalid rent amount");
            rentAmountEditText.requestFocus();
            return false;
        }

        // All validations passed
        return true;
    }

    private void updateFirestoreDocument(String propertyId, String title, String address, double rentAmount) {
        // Get Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Create a map with the updated fields
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("address", address);
        updates.put("rentAmount", rentAmount);

        // Update the document in the "properties" collection
        db.collection("properties")
                .document(propertyId) // Use the propertyId to identify the document
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Document updated successfully
                    Toast.makeText(getContext(), "Property updated successfully!", Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) {
                        getActivity().onBackPressed(); // Navigate back after update
                    }
                })
                .addOnFailureListener(e -> {
                    // Handle errors
                    Toast.makeText(getContext(), "Error updating property: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void deleteFirestoreDocument(String propertyId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Check if the property has an active tenant
        db.collection("properties")
                .document(propertyId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String tenantId = documentSnapshot.getString("tenantId");
                        if (tenantId != null && !tenantId.isEmpty()) {
                            // Property has an active tenant
                            Toast.makeText(getContext(), "Cannot delete property with an active tenant.", Toast.LENGTH_SHORT).show();
                        } else {
                            // No active tenant, proceed with deletion
                            confirmAndDeleteProperty(db, propertyId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error checking property: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmAndDeleteProperty(FirebaseFirestore db, String propertyId) {
        new AlertDialog.Builder(getContext())
                .setTitle("Delete Property")
                .setMessage("Are you sure you want to delete this property? This action cannot be undone.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Proceed with deletion
                    db.collection("properties")
                            .document(propertyId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Property deleted successfully!", Toast.LENGTH_SHORT).show();
                                if (getActivity() != null) {
                                    getActivity().onBackPressed(); // Navigate back after deletion
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Error deleting property: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("No", null)
                .show();
    }
}