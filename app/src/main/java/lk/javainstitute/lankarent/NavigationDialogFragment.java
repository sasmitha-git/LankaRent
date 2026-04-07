package lk.javainstitute.lankarent;

import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.w3c.dom.Text;

import lk.javainstitute.lankarent.navigation.AppTransactionsFragment;
import lk.javainstitute.lankarent.navigation.ProfileFragment;
import lk.javainstitute.lankarent.navigation.SecurityFragment;

public class NavigationDialogFragment extends DialogFragment {

    private View navContainer;
    private View backgroundDim;

    private  TextView nameText,phoneText;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = new Dialog(requireContext(), R.style.FullScreenDialogTheme);
        dialog.setContentView(R.layout.fragment_navigation_dialog);

        // Get views
        navContainer = dialog.findViewById(R.id.navContainer);
        backgroundDim = dialog.findViewById(R.id.backgroundDim);

        TextView profileText = dialog.findViewById(R.id.profileText);
        TextView security = dialog.findViewById(R.id.security);
        TextView transactionText = dialog.findViewById(R.id.TransactionText);
        TextView paymentText = dialog.findViewById(R.id.PaymentDetailsText);
        TextView inviteText = dialog.findViewById(R.id.InviteText);
        TextView settingText = dialog.findViewById(R.id.SettingText);
        TextView logOut = dialog.findViewById(R.id.logoutText);

         nameText = dialog.findViewById(R.id.nameText1);
         phoneText = dialog.findViewById(R.id.phoneText1);

         getUserData();

        profileText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFragment(new ProfileFragment());
                closeMenu();
            }
        });

        security.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFragment(new SecurityFragment());
                closeMenu();
            }
        });

        transactionText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openFragment(new AppTransactionsFragment());
                closeMenu();
            }
        });

        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                logoutUser();
            }
        });

        // Fade in background dim effect
        backgroundDim.setVisibility(View.VISIBLE);
        backgroundDim.setAlpha(0f);
        backgroundDim.animate().alpha(1f).setDuration(300).start();

        // Ensure width is measured before animation
        navContainer.post(() -> {
            // Slide in the menu from the right
            ObjectAnimator animator = ObjectAnimator.ofFloat(navContainer, "translationX", navContainer.getWidth(), 0f);
            animator.setDuration(300);
            animator.start();
        });

        // Close when tapping outside
        backgroundDim.setOnClickListener(v -> closeMenu());

        return dialog;
    }
    private void getUserData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference docRef = db.collection("users").document(uid);
            docRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String name = documentSnapshot.getString("name");
                    String phone = documentSnapshot.getString("phone");

                    nameText.setText(name != null ? name : "No name available");
                    phoneText.setText(phone != null ? phone : "No phone number available");
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to fetch user data", Toast.LENGTH_SHORT).show();
                Log.e("NavigationDialog", "Error fetching user data", e);
            });
        }
    }
    private void logoutUser() {
        // Check if a user is currently logged in
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(getContext(), "No user is currently logged in", Toast.LENGTH_SHORT).show();
        } else {
            // Sign out the user
            FirebaseAuth.getInstance().signOut();


            // Dismiss the dialog
            dismiss();

            // Redirect to the login screen
            Intent intent = new Intent(requireActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear the back stack
            startActivity(intent);

            // Show a toast message
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        }
    }


private void openFragment(Fragment fragment) {
    if (getActivity() instanceof TenantDashboardActivity) {
        TenantDashboardActivity activity = (TenantDashboardActivity) getActivity();
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.tenant_fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    } else if (getActivity() instanceof LandlordDashboardActivity) {
        LandlordDashboardActivity activity = (LandlordDashboardActivity) getActivity();
        FragmentTransaction transaction = activity.getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.landlord_fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    } else {
        Log.d("NavigationDialog", "Unknown Activity: " + getActivity().getClass().getSimpleName());
    }

    // Hide card views if applicable
    View cardView = getActivity().findViewById(R.id.cardView1);
    if (cardView != null) {
        cardView.setVisibility(View.GONE);
    }

    View tenantcardView = getActivity().findViewById(R.id.tenantcardView);
    if (tenantcardView != null) {
        tenantcardView.setVisibility(View.GONE);
    }
}
        private void closeMenu() {
        // Fade out background dim effect
        backgroundDim.animate().alpha(0f).setDuration(300).withEndAction(() -> backgroundDim.setVisibility(View.GONE)).start();

        // Slide out the menu to the right and dismiss after animation
        ObjectAnimator animator = ObjectAnimator.ofFloat(navContainer, "translationX", 0f, navContainer.getWidth());
        animator.setDuration(300);
        animator.start();
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                dismiss();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }
}