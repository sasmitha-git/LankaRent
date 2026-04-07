package lk.javainstitute.lankarent;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
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
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;


import model.Property;

public class LandlordDashboardActivity extends AppCompatActivity {

    private HomeListAdapter adapter;
    private final List<Property> propertyList = new ArrayList<>();
    private ListenerRegistration propertiesListener;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 123;

    private TextView textTotalProperties, textTotalRent,landlord;

    private FirebaseFirestore db;
    private CardView cardView1;
    private FirebaseAuth firebaseAuth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_landlord_dashboard);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.landlord_fragment_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        textTotalProperties = findViewById(R.id.totalPropertyAmount);
        textTotalRent = findViewById(R.id.totalRentAmount);
        landlord = findViewById(R.id.textView4);

        ImageView rentBoard = findViewById(R.id.imageView20);

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

        firebaseAuth = FirebaseAuth.getInstance();

        db = FirebaseFirestore.getInstance();
        checkEmailVerification();
         getLandLordHomes();
         
         getLandlordName();

        cardView1 = findViewById(R.id.cardView1);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Request the permission if not granted
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
            } else {
                // If permission is already granted, you can send notifications
                sendNotification(new Property());
            }
        }


        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {

            @Override
            public void handleOnBackPressed() {
                FragmentManager fragmentManager = getSupportFragmentManager();
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    fragmentManager.popBackStack();
                    findViewById(R.id.cardView1).setVisibility(View.VISIBLE);

                } else {
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.addCategory(Intent.CATEGORY_HOME);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            }
        });


        Button addHome = findViewById(R.id.addHomeButton);
        addHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // Hide the dashboard layout
                findViewById(R.id.cardView1).setVisibility(View.GONE);

                // Create a new instance of the fragment
                AddPropertyFragment fragment = new AddPropertyFragment();
                FragmentManager fragmentManager = getSupportFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.landlord_fragment_container, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }

        });


        RecyclerView recyclerView = findViewById(R.id.recycleView1);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HomeListAdapter(propertyList,cardView1);
        recyclerView.setAdapter(adapter);


        ImageButton menuButton = findViewById(R.id.menuButton);
        menuButton.setOnClickListener(new View.OnClickListener() {
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
                startActivity(new Intent(LandlordDashboardActivity.this, LoginActivity.class));
                finish();
            }
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
    private void getLandlordName() {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String landlordName = documentSnapshot.getString("name"); // Assuming 'name' field exists
                        if (landlordName != null) {
                            landlord.setText("Hi "+landlordName);
                        } else {
                            landlord.setText("Landlord"); // Default text if name is not available
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("LandlordDashboard", "Failed to fetch landlord name", e);
                    landlord.setText("Landlord"); // Set default text in case of failure
                });
    }
    private void getLandLordHomes() {
        // Get the current landlord's ID
        String landlordId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // Add a real-time listener to the properties collection
        db.collection("properties")
                .whereEqualTo("landlordId", landlordId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("LandlordDashboard", "Listen failed: ", error);
                        return;
                    }

                    if (value != null && !value.isEmpty()) {
                        int totalProperties = 0;
                        double totalRent = 0.0;

                        // Calculate total properties and total rent
                        for (QueryDocumentSnapshot document : value) {
                            totalProperties++;

                            // Use Double instead of double to handle null values
                            Double rentAmount = document.getDouble("rentAmount");

                            // Add rentAmount to totalRent if it's not null
                            if (rentAmount != null) {
                                totalRent += rentAmount;
                            }
                        }

                        // Update the UI
                        updateUI(totalProperties, totalRent);
                    } else {
                        // No properties found
                        updateUI(0, 0.0);
                    }
                });
    }
    private void updateUI(int totalProperties, double totalRent) {
        // Update total properties
        textTotalProperties.setText(String.valueOf(totalProperties));

        // Update total rent
        textTotalRent.setText("LKR."+ String.format("%.2f", totalRent));
    }





    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onStart() {
        super.onStart();
        // Attach real-time listener
        String currentUserID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        propertiesListener = db.collection("properties")
                .whereEqualTo("landlordId", currentUserID)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("LandlordDashboard", "Listen failed.", e);
                        return;
                    }
                    if (snapshots != null) {
                        propertyList.clear();
                        boolean notificationSent = false;

                        for (DocumentSnapshot document : snapshots.getDocuments()) {
                            Property property = document.toObject(Property.class);
                            if (property != null) {
                                propertyList.add(property);

                                // Check if there is any property with status "pending_approval"
                                if ("pending_approval".equals(property.getStatus()) && !notificationSent) {
                                    sendNotification(property);
                                    notificationSent = true; // Avoid multiple notifications at once
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });

        // Real-time listener for payments
        db.collection("payments")
                .whereEqualTo("landlordId", currentUserID)
                .whereEqualTo("notified", false)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("LandlordDashboard", "Payment listen failed", e);
                        return;
                    }

                    if (snapshots != null && !snapshots.isEmpty()) {
                        for (DocumentSnapshot documentSnapshot : snapshots.getDocuments()) {
                            String paymentId = documentSnapshot.getId();
                            String propertyId = documentSnapshot.getString("propertyId");
                            double amount = documentSnapshot.getDouble("amount");

                            if (propertyId != null) {
                                sendPaymentNotification(propertyId, amount);

                                // Mark payment as notified in Firestore
                                updatePaymentNotificationStatus(paymentId);

                            } else {
                                Log.e("LandlordDashboard", "Property ID is null for payment ID: " + paymentId);
                            }
                        }
                    } else {
                        Log.d("LandlordDashboard", "No new payments found");
                    }
                });

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults, int deviceId) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId);

        if(requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE){
            if(grantResults.length > 0 && grantResults [0] == PackageManager.PERMISSION_GRANTED){
                sendNotification(new Property());
            }else {
                Toast.makeText(this, "Permission denied to post notifications",Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        // Detach the listener to avoid leaks
        if (propertiesListener != null) {
            propertiesListener.remove();
            propertiesListener = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    private void sendNotification(Property property) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return; // Exit if permission is not granted
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create notification channel for Android 8.0 (API level 26) and above
            CharSequence name = "Property Notification";
            String description = "Notification for Pending property approvals";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("property_channel", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "property_channel")
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("Property Pending Approval")
                .setContentText("Your property \"" + property.getTitle() + "\" is pending approval.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // Trigger the notification
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify((int) System.currentTimeMillis(), builder.build());
    }

    private void sendPaymentNotification(String propertyId, double amount) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return; // Exit if permission is not granted
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create notification channel for Android 8.0+
            CharSequence name = "Payment Notification";
            String description = "Notification for tenant payments";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("payment_channel", name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "payment_channel")
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("New Rent Payment Received")
                .setContentText("Payment of Rs." + amount + " received for Property ID: " + propertyId)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // Trigger the notification
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify((int) System.currentTimeMillis(), builder.build());
    }

    private  void updatePaymentNotificationStatus(String paymentId){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("payments").document(paymentId)
                .update("notified",true)
                .addOnSuccessListener(avoid -> Log.d("LandlordDashboard","payment" +paymentId +"marked as notified"))
                .addOnFailureListener(e -> Log.e("landlordDashboard","Failed to update notified status",e));
    }
}

class HomeListAdapter extends RecyclerView.Adapter<HomeListAdapter.PropertyViewHolder>{

    static class PropertyViewHolder extends  RecyclerView.ViewHolder{

        TextView  textViewHomeTitle;
        TextView  textViewCity;
        Button buttonConfirm;
        ImageButton buttonShare;

        public PropertyViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewHomeTitle =itemView.findViewById(R.id.titleHomeTextView);
            textViewCity = itemView.findViewById(R.id.cityTitleTextView);
            buttonConfirm =itemView.findViewById(R.id.buttonConfirm);
            buttonShare = itemView.findViewById(R.id.sharePropIdButton);

        }
    }

    private final List<Property> propertyList;

    private  CardView cardView1;

    public HomeListAdapter(List<Property> propertyList, CardView cardView1) {
        this.propertyList = propertyList;
        this.cardView1 = cardView1;

    }

    @NonNull
    @Override
    public PropertyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View propertyView = layoutInflater.inflate(R.layout.home_item_landloard,parent,false);
        PropertyViewHolder propertyViewHolder = new PropertyViewHolder(propertyView);
        return propertyViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull PropertyViewHolder holder, int position) {
        Property property = propertyList.get(position);
        holder.textViewHomeTitle.setText(property.getTitle());
        holder.textViewCity.setText(property.getCity());

        // Handle confirmation button visibility
        if (property.getTenantId() != null && property.getStatus().equals("pending_approval")) {
            holder.buttonConfirm.setVisibility(View.VISIBLE);
            holder.buttonConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    Calendar calendar = Calendar.getInstance();
                    int rentStartMonth = calendar.get(Calendar.MONTH) + 1;
                    int rentStartYear = calendar.get(Calendar.YEAR);

                    db.collection("properties").document(property.getPropertyId())
                            .update("status", "approved",
                                    "rentStartMonth", rentStartMonth,
                                    "rentStartYear", rentStartYear)
                            .addOnSuccessListener(unused -> {
                                holder.buttonConfirm.setVisibility(View.GONE);
                                holder.buttonShare.setEnabled(false); // Disable share button
                                holder.buttonShare.setAlpha(0.5f); // Dim the button to indicate it's disabled
                                Toast.makeText(view.getContext(), "Tenant confirmed", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(view.getContext(), "Failed to confirm tenant", Toast.LENGTH_SHORT).show()
                            );
                }
            });
        } else {
            holder.buttonConfirm.setVisibility(View.GONE);
        }

        // Disable Share button if property is approved
        if ("approved".equals(property.getStatus())) {
            holder.buttonShare.setEnabled(false);
            holder.buttonShare.setAlpha(0.5f); // Make it look disabled
        } else {
            holder.buttonShare.setEnabled(true);
            holder.buttonShare.setAlpha(1.0f);
        }

        // Share button functionality
        holder.buttonShare.setOnClickListener(view -> {
            if (holder.buttonShare.isEnabled()) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, property.getPropertyId());
                view.getContext().startActivity(Intent.createChooser(shareIntent, "Share property"));
            }
        });

        //Handle item click to navigate to AddPropertyFragment
        holder.itemView.setOnClickListener(view -> {

            if(cardView1 != null){
                cardView1.setVisibility(View.GONE);
            }

            // Create a bundle to pass the property data
            Bundle bundle = new Bundle();
            bundle.putString("propertyId",property.getPropertyId());
            bundle.putString("title", property.getTitle());
            bundle.putString("address", property.getAddress());
            bundle.putDouble("rentAmount", property.getRentAmount());


            // Navigate to AddPropertyFragment
            EditHomeFragment fragment = new EditHomeFragment();
            fragment.setArguments(bundle);

            // Use FragmentManager to replace the current fragment
            // fragmentManager to replace the current fragment
            FragmentManager fragmentManager = ((AppCompatActivity) view.getContext()).getSupportFragmentManager();
            fragmentManager.beginTransaction()
                    .replace(R.id.landlord_fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
        });
    }

    @Override
    public int getItemCount() {
        return propertyList.size();
    }
}