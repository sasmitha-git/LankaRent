package lk.javainstitute.lankarent;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import lk.payhere.androidsdk.PHMainActivity;
import model.Payment;
import service.PayHere;

public class PaymentConfirmationDialog extends DialogFragment {

    private static final String ARG_TITLE = "title";
    private static final String ARG_CITY = "city";
    private static final String ARG_RENT_AMOUNT = "rentAmount";
    private static final String ARG_LANDLORD_NAME = "landlordName";
    private static final String ARG_LANDLORD_EMAIL = "landlordEmail";
    private static final String ARG_LANDLORD_PHONE = "landlordPhone";

    private  static  final String ARG_DUE_MONTH = "dueMonth";


    public static PaymentConfirmationDialog newInstance(String title, String city, String rent,
                                                        String landlordName, String landlordEmail,
                                                        String landlordPhone,String landlordId,String dueMonth) {

        PaymentConfirmationDialog dialog = new PaymentConfirmationDialog();
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_CITY, city);
        args.putString(ARG_RENT_AMOUNT , rent);
        args.putString(ARG_LANDLORD_NAME, landlordName);
        args.putString(ARG_LANDLORD_EMAIL, landlordEmail);
        args.putString(ARG_LANDLORD_PHONE, landlordPhone);
        args.putString("landlordId",landlordId);
        args.putString(ARG_DUE_MONTH,dueMonth);
        dialog.setArguments(args);
        return dialog;
    }
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_payment_confirmation,null);

        TextView titleText = view.findViewById(R.id.titleText);
        TextView cityText = view.findViewById(R.id.cityText);
        TextView rentAmountText = view.findViewById(R.id.rentAmountText);
        TextView landlordNameText = view.findViewById(R.id.landlordNameText);
        TextView landlordEmailText = view.findViewById(R.id.landlordEmailText);
        TextView landlordPhoneText = view.findViewById(R.id.landlordPhoneText);
        TextView dueMonthText = view.findViewById(R.id.dueMonthText);

        titleText.setText(getArguments().getString(ARG_TITLE));
        cityText.setText(getArguments().getString(ARG_CITY));
        rentAmountText.setText(getArguments().getString(ARG_RENT_AMOUNT));
        landlordNameText.setText(getArguments().getString(ARG_LANDLORD_NAME));
        landlordEmailText.setText(getArguments().getString(ARG_LANDLORD_EMAIL));
        landlordPhoneText.setText(getArguments().getString(ARG_LANDLORD_PHONE));
        dueMonthText.setText(getArguments().getString(ARG_DUE_MONTH));

        // Initialize custom buttons
        MaterialButton confirmButton = view.findViewById(R.id.confirmButton);
        MaterialButton cancelButton = view.findViewById(R.id.cancelButton);

        // Set click listeners for custom buttons
        confirmButton.setOnClickListener(v -> proceedToPayment());
        cancelButton.setOnClickListener(v -> dismiss());

        // Set the custom view
        builder.setView(view);


        return builder.create();

    }

    private void proceedToPayment() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String tenantId = currentUser.getUid();
        String propertyId = getArguments().getString(ARG_TITLE);
        double amount = Double.parseDouble(getArguments().getString(ARG_RENT_AMOUNT).replace("Rs.", ""));

        String landlordId = getArguments().getString("landlordId");
        // Extract month and year from dueMonth
        String dueMonthStr = getArguments().getString(ARG_DUE_MONTH);

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
            Date date = dateFormat.parse(dueMonthStr);

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            int month = calendar.get(Calendar.MONTH) + 1; // Convert zero-based month to 1-based
            int year = calendar.get(Calendar.YEAR);

            Intent intent = new Intent(getActivity(), PayHere.class);
            intent.putExtra("tenantId", tenantId);
            intent.putExtra("landlordId",landlordId);
            intent.putExtra("propertyId",propertyId);
            intent.putExtra("amount", amount);
            intent.putExtra("month", month);
            intent.putExtra("year", year);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
