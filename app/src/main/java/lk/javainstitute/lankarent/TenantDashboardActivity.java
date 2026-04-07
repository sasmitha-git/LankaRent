package lk.javainstitute.lankarent;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import model.Property;

public class TenantDashboardActivity extends AppCompatActivity {


    private CardView homeCardView;

    private RentalHomeListAdapter adapter;
    private EditText homeIDText;

    private FirebaseFirestore db;
    private TextView tenant;
    private final List<Property> homeList = new ArrayList<>();
    private FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tenant_dashboard);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.tenant_fragment_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        checkEmailVerification();

        tenant = findViewById(R.id.textView19);
        homeCardView = findViewById(R.id.tenantcardView);
        db = FirebaseFirestore.getInstance();

        createNotificationChannel();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
                return;
            }
        }


        ImageView rentBoard = findViewById(R.id.imageView18);

        ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(rentBoard, "rotation", -5f, 5f);
        rotateAnimator.setDuration(3000);
        rotateAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        rotateAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        rotateAnimator.setInterpolator(new LinearInterpolator());

        ObjectAnimator translateXAnimator = ObjectAnimator.ofFloat(rentBoard, "translationX", -10f, 10f);
        translateXAnimator.setDuration(3000);
        translateXAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        translateXAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        translateXAnimator.setInterpolator(new LinearInterpolator());

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(rotateAnimator, translateXAnimator);
        animatorSet.start();


        getTenantName();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {

            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    homeCardView.setVisibility(View.VISIBLE);
                    getSupportFragmentManager().popBackStack();
                } else {
                    finish();
                }
            }
        });


        homeIDText = findViewById(R.id.editTextHomeID);

        Button submitButton = findViewById(R.id.buttonSubmit);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                claimHome();
            }
        });

        RecyclerView recyclerView = findViewById(R.id.recycleView2);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RentalHomeListAdapter(homeList, this);
        recyclerView.setAdapter(adapter);

        ImageButton button = findViewById(R.id.menuButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavigationDialogFragment navigationDialogFragment = new NavigationDialogFragment();
                navigationDialogFragment.show(getSupportFragmentManager(), "NavigationDialog");
            }
        });


    }

    private void checkEmailVerification() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            if (!user.isEmailVerified()) {
                // Email is not verified, redirect to login and sign out
                showCustomToast("Email is not verified!", R.drawable.add_user);
                firebaseAuth.signOut();
                startActivity(new Intent(TenantDashboardActivity.this, LoginActivity.class));
                finish();
            }
        } else {
            // No user is logged in, redirect to login
            startActivity(new Intent(TenantDashboardActivity.this, LoginActivity.class));
            finish();
        }
    }
    public void showCustomToast(String message, int iconResId) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.custom_toast_layout,
                findViewById(R.id.custom_toast_container));

        ImageView toastIcon = layout.findViewById(R.id.toast_icon);
        TextView toastText = layout.findViewById(R.id.toast_text);

        toastIcon.setImageResource(iconResId);
        toastText.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
    private void getTenantName() {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String tenantName = documentSnapshot.getString("name"); // Assuming 'name' field exists
                        if (tenantName != null) {
                            tenant.setText("Hi "+tenantName);
                        } else {
                            tenant.setText("Tenant"); // Default text if name is not available
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("LandlordDashboard", "Failed to fetch landlord name", e);
                    tenant.setText("Tenant"); // Set default text in case of failure
                });
    }


    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onStart() {
        super.onStart();

        // Fetch the current tenant's ID
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetch approved properties for the tenant
        db.collection("properties")
                .whereEqualTo("tenantId", currentUserId)
                .whereEqualTo("status", "approved")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("TenantDashboard", "Listen failed.", e);
                        return;
                    }
                    if (snapshots != null && !snapshots.isEmpty()) {
                        homeList.clear();
                        for (DocumentSnapshot documentSnapshot : snapshots.getDocuments()) {
                            Property property = documentSnapshot.toObject(Property.class);
                            if (property != null) {
                                homeList.add(property);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });

        db.collection("payments")
                .whereEqualTo("tenantId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        String propertyId = document.getString("propertyId");
                        Timestamp dueDateTimestamp = document.getTimestamp("dueDate");
                        if (dueDateTimestamp != null) {
                            long dueDateMillis = dueDateTimestamp.toDate().getTime();
                            long currentMillis = System.currentTimeMillis();

                            // Check if the due date is within the next 7 days
                            if (dueDateMillis > currentMillis && dueDateMillis <= currentMillis + (7 * 24 * 60 * 60 * 1000)) {
                                sendPaymentReminderNotification(propertyId, dueDateMillis);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TenantDashboard", "Error fetching payment details", e);
                });



    }

    //Start Notification//
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Payment Reminder";
            String description = "Channel for payment reminders";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("payment_reminder", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void sendPaymentReminderNotification(String propertyId, long dueDateMillis) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "payment_reminder")
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("Payment Due Soon")
                .setContentText("Your payment for property " + propertyId + " is due soon.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // Request the permission if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            return;
        }
        notificationManager.notify(propertyId.hashCode(), builder.build());
    }
    //End Notification//



    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
//        findViewById(R.id.recycleView2).setVisibility(View.VISIBLE);

    }



    private void claimHome() {
        String homeID = homeIDText.getText().toString();

        if (homeID.isEmpty()) {
            Toast.makeText(this, "Please enter home ID", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        String tenantId = auth.getCurrentUser().getUid();

        DocumentReference homeRef = firestore.collection("properties").document(homeID);

        // Check if the property is already claimed
        homeRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String existingTenantId = documentSnapshot.getString("tenantId");
                String status = documentSnapshot.getString("status");

                // If already claimed by another tenant
                if (existingTenantId != null && !existingTenantId.isEmpty() && !existingTenantId.equals(tenantId)) {
                    Toast.makeText(this, "This property is already claimed by another tenant.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // If the current tenant has already claimed the property
                if ("pending_approval".equals(status)) {
                    Toast.makeText(this, "You have already claimed this property. Awaiting approval.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if ("approved".equals(status)) {
                    Toast.makeText(this, "You have already claimed this property.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Proceed with claiming the property
                homeRef.update("tenantId", tenantId, "status", "pending_approval")
                        .addOnSuccessListener(unused ->
                                Toast.makeText(this, "Property claimed successfully. Waiting for landlord approval.", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Error claiming property: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Property not found.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Error retrieving property: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }


}


class RentalHomeListAdapter extends RecyclerView.Adapter<RentalHomeListAdapter.RentalHomeViewHolder> {

    static class RentalHomeViewHolder extends RecyclerView.ViewHolder {

        TextView homeTitle;
        TextView cityText;
        ImageButton buttonCall;

        public RentalHomeViewHolder(@NonNull View itemView) {
            super(itemView);

            homeTitle = itemView.findViewById(R.id.homeTitle);
            cityText = itemView.findViewById(R.id.cityText);
            buttonCall = itemView.findViewById(R.id.buttonCall);

        }
    }

    private final List<Property> propertyList;
    private final TenantDashboardActivity activity;

    public RentalHomeListAdapter(List<Property> propertyList, TenantDashboardActivity activity) {
        this.propertyList = propertyList;
        this.activity = activity;
    }

    @NonNull
    @Override
    public RentalHomeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View rentHomeView = inflater.inflate(R.layout.home_item_tenant, parent, false);
        RentalHomeViewHolder rentalHomeViewHolder = new RentalHomeViewHolder(rentHomeView);
        return rentalHomeViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RentalHomeViewHolder holder, int position) {

        Property property = propertyList.get(position);
        holder.homeTitle.setText(property.getTitle());
        holder.cityText.setText(property.getCity());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openHomeDetailFragment(view, property.getPropertyId());
            }
        });
        holder.buttonCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fetchLandLordPhone(property.getLandlordId(), view);
            }
        });
    }

    @Override
    public int getItemCount() {
        return propertyList.size();
    }

    private void fetchLandLordPhone(String landLordId, View view) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference landlordRef = db.collection("users").document(landLordId);
        landlordRef.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    String phone = documentSnapshot.getString("phone");
                    if (phone != null && !phone.isEmpty()) {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone));
                        view.getContext().startActivity(intent);
                    } else {
                        Toast.makeText(view.getContext(), "Phone number not found", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(view.getContext(), "Landlord not found", Toast.LENGTH_SHORT).show();

                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(view.getContext(), "Error fetching landlord phone", Toast.LENGTH_SHORT).show();

            }
        });
    }


    private void openHomeDetailFragment(View view, String homeId) {
        CardView homeCardView = activity.findViewById(R.id.tenantcardView);
        if (homeCardView != null) {
            homeCardView.setVisibility(View.GONE);
        }

        Fragment homeDetailFragment = HomeDetailFragment.newInstance(homeId);
        activity.getSupportFragmentManager().beginTransaction()
                .replace(R.id.tenant_fragment_container, homeDetailFragment)
                .addToBackStack(null)
                .commit();
    }

}