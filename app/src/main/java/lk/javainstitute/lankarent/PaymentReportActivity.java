package lk.javainstitute.lankarent;

import static android.content.ContentValues.TAG;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaActionSound;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class PaymentReportActivity extends AppCompatActivity {

    private TextView priceTag, refNum ,date,time,tenantName ,landlordName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_payment_report);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        priceTag = findViewById(R.id.priceTagTextView);
        refNum = findViewById(R.id.refNumberTextView);
        date = findViewById(R.id.dateTextView);
        time = findViewById(R.id.timeTextView);
        tenantName = findViewById(R.id.tenantNametextView);
        landlordName = findViewById(R.id.landlordTextview);

        Intent i = getIntent();
        double amount = i.getDoubleExtra("amount", 0.0);
        String tenantId = i.getStringExtra("tenantId");
        String landlordId = i.getStringExtra("landlordId");
        String propertyId = i.getStringExtra("propertyId");
        int month = i.getIntExtra("month", 1);
        int year = i.getIntExtra("year", 2025);
        String paymentId = i.getStringExtra("paymentId");

        priceTag.setText(String.format("LKR %.2f", amount));
        refNum.setText(paymentId);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        assert tenantId != null;
        db.collection("users").document(tenantId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String tenantNameStr = documentSnapshot.getString("name");
                        tenantName.setText(tenantNameStr);
                    }
                });
        assert landlordId != null;
        db.collection("users").document(landlordId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String landlordStr = documentSnapshot.getString("name");
                    landlordName.setText(landlordStr);
                });

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(calendar.getTime());
        date.setText(currentDate);

        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        String currentTime = timeFormat.format(calendar.getTime());
        time.setText(currentTime);

        Button pdfDownload = findViewById(R.id.pdfButton);
        pdfDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Implement PDF download functionality here
                try {
                    View content = findViewById(R.id.paymentCardView);
                    Bitmap bitmap = getBitmapFromView(content);
                    createPdf(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Button homeButton = findViewById(R.id.button5);
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(PaymentReportActivity.this, TenantDashboardActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private Bitmap getBitmapFromView(View view){
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(),view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    private void createPdf(Bitmap bitmap) throws IOException{

        String fileName = "payment_report_" + System.currentTimeMillis() +".pdf";
        ContentResolver contentResolver = getContentResolver();
        Uri pdfUri = null;
        OutputStream outputStream = null;


        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Downloads.DISPLAY_NAME,fileName);
            contentValues.put(MediaStore.Downloads.MIME_TYPE,"application/pdf");
            contentValues.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS);

            pdfUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,contentValues);
        }else{
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME,fileName);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE,"application/pdf");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS);

            pdfUri = contentResolver.insert(MediaStore.Files.getContentUri("external"),contentValues);

        }

        if(pdfUri != null){
            outputStream = contentResolver.openOutputStream(pdfUri);
            if(outputStream != null){
                PdfWriter pdfWriter = new PdfWriter(outputStream);
                PdfDocument pdfDocument = new PdfDocument(pdfWriter);
                Document document = new Document(pdfDocument);


                //Convert  bitmap to have array
                ByteArrayOutputStream outputStreamBitMap = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG,100,outputStreamBitMap);
                byte[] byteArray = outputStreamBitMap.toByteArray();

                ImageData imageData = ImageDataFactory.create(byteArray);
                Image image = new Image(imageData);
                document.add(image);

                document.close();
                outputStream.close();

                Log.d(TAG,"PDF created successfully"+pdfUri.toString());

                //open pdf after saving
                openPdf(pdfUri);
            }
        }
    }

    private  void openPdf(Uri pdfUri){
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(pdfUri,"application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(Intent.createChooser(intent,"Open PDF"));
        }catch (ActivityNotFoundException e){
            Log.i(TAG,"No PDF viewer installed!" +e);
        }
    }
}