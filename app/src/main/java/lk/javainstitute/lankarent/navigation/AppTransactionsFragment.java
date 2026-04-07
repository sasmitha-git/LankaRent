package lk.javainstitute.lankarent.navigation;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import lk.javainstitute.lankarent.LandlordDashboardActivity;
import lk.javainstitute.lankarent.R;
import lk.javainstitute.lankarent.TenantDashboardActivity;
import model.Payment;


public class AppTransactionsFragment extends Fragment {

    private FirebaseAuth mAuth;
    private String currentUserId;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_transactions, container, false);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
        }

        // Initialize RecyclerView and Adapter
        RecyclerView recyclerView = view.findViewById(R.id.paymentRecycleView);
        PaymentsAdapter adapter = new PaymentsAdapter(new ArrayList<>());

        // Set LayoutManager to RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Fetch payments based on the current user
        getPayments(currentUserId, adapter);

        // Back button logic
        ImageView backButton = view.findViewById(R.id.imageView13);
        backButton.setOnClickListener(view1 -> {
            if (getActivity() instanceof TenantDashboardActivity) {
                Intent intent = new Intent(getActivity(), TenantDashboardActivity.class);
                startActivity(intent);
                getActivity().finish();
            } else if (getActivity() instanceof LandlordDashboardActivity) {
                Intent intent = new Intent(getActivity(), LandlordDashboardActivity.class);
                startActivity(intent);
                getActivity().finish();
            }
        });

        return view;
    }

    private void getPayments(String userId, PaymentsAdapter adapter) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        ArrayList<Payment> allPayments = new ArrayList<>();

        db.collection("payments")
                .whereEqualTo("tenantId", userId)
                .addSnapshotListener((tenantSnapshot, error) -> {
                    if (error != null) {
                        Log.d("FireStore", "Error getting tenant payments: ", error);
                        return;
                    }
                    if (tenantSnapshot != null) {
                        allPayments.clear();
                        for (QueryDocumentSnapshot document : tenantSnapshot) {
                            Payment payment = document.toObject(Payment.class);
                            allPayments.add(payment);
                        }
                    }

                    db.collection("payments")
                            .whereEqualTo("landlordId", userId)
                            .addSnapshotListener((landlordSnapshot, error2) -> {
                                if (error2 != null) {
                                    Log.d("FireStore", "Error getting landlord payments: ", error2);
                                    return;
                                }
                                if (landlordSnapshot != null) {
                                    for (QueryDocumentSnapshot document : landlordSnapshot) {
                                        Payment payment = document.toObject(Payment.class);
                                        allPayments.add(payment);
                                    }
                                }
                               adapter.setPayments(allPayments);
                            });
                });
    }
}

class PaymentsAdapter extends RecyclerView.Adapter<PaymentsAdapter.PaymentsViewHolder>{

    static  class PaymentsViewHolder extends RecyclerView.ViewHolder{
        TextView  dateTime;
        TextView tenantName;
        TextView rentPrice;
        TextView landlordname;
        public PaymentsViewHolder(@NonNull View itemView) {
            super(itemView);

            dateTime = itemView.findViewById(R.id.textView40);
            tenantName = itemView.findViewById(R.id.tenant_name_text);
            rentPrice = itemView.findViewById(R.id.priceTextView);
            landlordname = itemView.findViewById(R.id.landlordName);
        }
    }
    ArrayList<Payment> paymentArrayList;

    public PaymentsAdapter(ArrayList<Payment> paymentArrayList) {
        this.paymentArrayList = paymentArrayList;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setPayments(ArrayList<Payment> payments) {
        this.paymentArrayList.clear();
        this.paymentArrayList.addAll(payments);
        notifyDataSetChanged(); // Notify specific range
    }

    @NonNull
    @Override
    public PaymentsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View paymentView =  layoutInflater.inflate(R.layout.payment_item,parent,false);
        PaymentsViewHolder paymentsViewHolder = new PaymentsViewHolder(paymentView);

        return paymentsViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull PaymentsViewHolder holder, int position) {

        Payment payment = paymentArrayList.get(position);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(payment.getTimeStamp());

        holder.dateTime.setText(formattedDate);

        // Format the amount with commas and "LKR" prefix
        DecimalFormat decimalFormat = new DecimalFormat("#,###");
        String formattedAmount = decimalFormat.format(payment.getAmount());
        String amountWithCurrency = "LKR " + formattedAmount;
        holder.rentPrice.setText(amountWithCurrency);

        //set tenant & landlord names
        getUserName(payment.getTenantId(),holder.tenantName);
        getUserName(payment.getLandlordId(),holder.landlordname);
    }

    @Override
    public int getItemCount() {
        return paymentArrayList.size();
    }

    private void getUserName(String userId, TextView textView){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()){
                        String name = documentSnapshot.getString("name");
                        textView.setText(name);
                    }
                }).addOnFailureListener(e->Log.d("FireStore","Error getting user name"));
    }
}

