package service;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import lk.javainstitute.lankarent.PaymentReportActivity;
import lk.javainstitute.lankarent.ReminderScheduler;
import lk.payhere.androidsdk.PHConfigs;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.Item;
import lk.payhere.androidsdk.model.StatusResponse;
import model.Payment;


public class PayHere extends AppCompatActivity {

    private static final String TAG = "PayHere";
    private FirebaseFirestore db;
    private String tenantId, propertyId, landlordId;
    private double amount;
    private int month, year;

    private final ActivityResultLauncher<Intent> payHereLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    if (data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                        Serializable serializable = data.getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);
                        if (serializable instanceof PHResponse) {
                            PHResponse<StatusResponse> response = (PHResponse<StatusResponse>) serializable;
                            if (response.isSuccess()) {
                                Toast.makeText(this, "Payment Successful!", Toast.LENGTH_SHORT).show();

                                // Call storePaymentHistory and handle the transition to PaymentReportActivity
                                storePaymentHistory();
                            } else {
                                Toast.makeText(this, "Payment Failed!", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    Toast.makeText(this, "User cancelled the payment", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Payment Failed: No data received");
                    Toast.makeText(this, "Payment Failed: No data received", Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            tenantId = user.getUid();
            Log.d(TAG, "Tenant ID: " + tenantId);
        } else {
            Toast.makeText(this, "User not logged in!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Intent intent = getIntent();
        tenantId = intent.getStringExtra("tenantId");
        landlordId = intent.getStringExtra("landlordId");
        propertyId = intent.getStringExtra("propertyId");
        amount = intent.getDoubleExtra("amount", 0.0);
        month = intent.getIntExtra("month", 1);
        year = intent.getIntExtra("year", 2024);

        Log.d(TAG, "Starting payment process");
        initiatePayment();
    }

    private void initiatePayment() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String fullName = user.getDisplayName();
            String firstName = "Tenant";
            String lastName = "";

            if (fullName != null) {
                String[] nameParts = fullName.split(" ", 2);
                firstName = nameParts[0];
                if (nameParts.length > 1) lastName = nameParts[1];
            }

            if (amount <= 0) {
                Toast.makeText(this, "Invalid payment amount!", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Invalid payment amount: " + amount);
                return;
            }

            InitRequest req = new InitRequest();
            req.setMerchantId("1228887");
            req.setCurrency("LKR");
            req.setAmount(amount);
            req.setOrderId(UUID.randomUUID().toString());
            req.setItemsDescription("Rental Payment");
            req.setCustom1(propertyId);
            req.setCustom2("Tenant's monthly payment");
            req.getCustomer().setFirstName(firstName);
            req.getCustomer().setLastName(lastName);
            req.getCustomer().setEmail(user.getEmail());
            req.getCustomer().setPhone(user.getPhoneNumber());
            req.getCustomer().getAddress().setAddress("No.01, Kandy Road");
            req.getCustomer().getAddress().setCity("kandy");
            req.getCustomer().getAddress().setCountry("Sri Lanka");

            // Optional Params
            req.getCustomer().getDeliveryAddress().setAddress("No.2, kandy Road");
            req.getCustomer().getDeliveryAddress().setCity("kandy");
            req.getCustomer().getDeliveryAddress().setCountry("Sri Lanka");
            req.getItems().add(new Item(null, "Rent Pay", 1, 100.0));

            try {
                Intent intent = new Intent(this, PHMainActivity.class);
                intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);

                // Enable sandbox mode
                PHConfigs.setBaseUrl(PHConfigs.SANDBOX_URL);

                payHereLauncher.launch(intent);
            } catch (Exception e) {
                Log.e(TAG, "Payment initialization error", e);
                Toast.makeText(this, "Payment initialization error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void storePaymentHistory() {

        boolean notified = false;

        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 1); // month is 0-based in Calendar
        calendar.add(Calendar.MONTH, 1); // Move to the next month
        Date dueDate = calendar.getTime();

        Payment payment = new Payment(tenantId, landlordId, propertyId, amount, month, year, null,dueDate, notified);

        db.collection("payments")
                .add(payment)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Payment recorded successfully!", Toast.LENGTH_SHORT).show();


                    // Get the payment ID
                    String paymentId = documentReference.getId();


                    // Create an Intent to open PaymentReportActivity
                    Intent intent = new Intent(PayHere.this, PaymentReportActivity.class);
                    intent.putExtra("paymentId", paymentId);
                    intent.putExtra("amount", amount);
                    intent.putExtra("tenantId", tenantId);
                    intent.putExtra("landlordId", landlordId);
                    intent.putExtra("month", month);
                    intent.putExtra("year", year);

                    // Start the PaymentReportActivity
                    startActivity(intent);

                    // Finish the current activity (optional)
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to record payment.", Toast.LENGTH_SHORT).show();
                });
    }

    private long getNextPaymentDueDate(String propertyId) {
        try {
            Query query = db.collection("payments")
                    .whereEqualTo("propertyId", propertyId)
                    .orderBy("dueDate", Query.Direction.DESCENDING)
                    .limit(1);

            QuerySnapshot querySnapshot = Tasks.await(query.get());

            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot documentSnapshot = querySnapshot.getDocuments().get(0);
                Payment lastPayment = documentSnapshot.toObject(Payment.class);

                if (lastPayment != null) {
                    Calendar calendar = Calendar.getInstance();
                    if (lastPayment.getDueDate() != null) { // Handle the first payment case
                        calendar.setTime(lastPayment.getDueDate());
                        calendar.add(Calendar.MONTH, 1);
                    } else { // First payment, due date is the 5th of the next month
                        calendar.add(Calendar.MONTH, 1); // Next month
                        calendar.set(Calendar.DAY_OF_MONTH, 5); // 5th of the month
                    }

                    // ***FOR TESTING ONLY: Set the due date to a few seconds in the future***
                    calendar.add(Calendar.SECOND, 10); // 10 seconds from now

                    // ***For production, use the actual calculation:***
                    //return calendar.getTimeInMillis();

                    return calendar.getTimeInMillis();
                }
            } else {
                // No previous payments, due date is the 5th of the next month
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MONTH, 1); // Next month
                calendar.set(Calendar.DAY_OF_MONTH, 5); // 5th of the month
                return calendar.getTimeInMillis();
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e("APP", "error fetching next due date", e);
        }
        return 0;
    }

}