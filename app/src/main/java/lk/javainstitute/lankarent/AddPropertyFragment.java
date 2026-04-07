package lk.javainstitute.lankarent;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import model.Property;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class AddPropertyFragment extends Fragment implements MapDialogFragment.OnLocationSelectedListener, SensorEventListener {

    // Cloudinary config
    private static final String CLOUDINARY_CLOUD_NAME = "dfbwvxnkf";
    private static final String CLOUDINARY_UPLOAD_PRESET = "k9s91s8s";
    private static final String CLOUDINARY_UPLOAD_URL =
            "https://api.cloudinary.com/v1_1/" + CLOUDINARY_CLOUD_NAME + "/image/upload";

    private EditText titleEditText, addressEditText, rentAmountEditText;
    private LinearLayout imageContainer;
    private LatLng selectedLatLng;
    private List<String> imagePaths = new ArrayList<>();

    private Spinner citySpinner;
    private final List<String> cities = Arrays.asList("Colombo", "Kandy", "Galle", "Jaffna", "Matara", "Ratnapura");
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean isShaking = false;
    private static final float SHAKE_THRESHOLD = 5.0f;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference propertiesCollection = db.collection("properties");

    public AddPropertyFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_property, container, false);
    }

    @Override
    public void onLocationSelected(LatLng latLng) {
        selectedLatLng = latLng;
        saveDraft();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        titleEditText = view.findViewById(R.id.propertyTitle);
        addressEditText = view.findViewById(R.id.propertyAddress);
        rentAmountEditText = view.findViewById(R.id.rentAmount);
        citySpinner = view.findViewById(R.id.citySpinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, cities);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        citySpinner.setAdapter(adapter);

        loadDraft();

        ImageButton backButton = view.findViewById(R.id.imageButton1);
        backButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        Button addImageButton = view.findViewById(R.id.addImagesButton);
        addImageButton.setOnClickListener(v -> {
            AddImageBottomSheet bottomSheet = new AddImageBottomSheet();
            bottomSheet.setLocationInstructionFlag(false);
            bottomSheet.show(getChildFragmentManager(), bottomSheet.getTag());
        });

        Button mapButton = view.findViewById(R.id.mapButton);
        mapButton.setOnClickListener(v -> {
            String selectedCity = citySpinner.getSelectedItem().toString();
            MapDialogFragment mapDialogFragment = new MapDialogFragment();
            Bundle bundle = new Bundle();
            bundle.putString("city_name", selectedCity);
            mapDialogFragment.setArguments(bundle);
            mapDialogFragment.show(getChildFragmentManager(), "mapDialog");
        });

        TextView questionMarkButton = view.findViewById(R.id.questionMarkButton);
        questionMarkButton.setOnClickListener(v -> {
            AddImageBottomSheet bottomSheet = new AddImageBottomSheet();
            bottomSheet.setLocationInstructionFlag(true);
            bottomSheet.show(getChildFragmentManager(), bottomSheet.getTag());
        });

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        float savedLat = sharedPreferences.getFloat("lat", 0);
        float savedLng = sharedPreferences.getFloat("lng", 0);

        if (savedLat != 0.0 && savedLng != 0.0) {
            selectedLatLng = new LatLng(savedLat, savedLng);
        }

        Button saveDetailsBtn = view.findViewById(R.id.btnUpdateProperty);
        saveDetailsBtn.setOnClickListener(v -> {

            String title = titleEditText.getText().toString();
            String rentAmountStr = rentAmountEditText.getText().toString();
            String address = addressEditText.getText().toString();
            String city = citySpinner.getSelectedItem().toString();
            imageContainer = requireView().findViewById(R.id.imageContainer);

            if (title.isEmpty()) {
                Toast.makeText(getContext(), "Please fill property title", Toast.LENGTH_LONG).show();
                return;
            }
            if (rentAmountStr.isEmpty()) {
                Toast.makeText(getContext(), "Please fill rent amount", Toast.LENGTH_LONG).show();
                return;
            }

            double rentAmount;
            try {
                rentAmount = Double.parseDouble(rentAmountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid rent amount", Toast.LENGTH_LONG).show();
                return;
            }

            if (address.isEmpty()) {
                Toast.makeText(getContext(), "Please fill the address", Toast.LENGTH_LONG).show();
                return;
            }
            if (city.isEmpty()) {
                Toast.makeText(getContext(), "Please select a city", Toast.LENGTH_LONG).show();
                return;
            }
            if (selectedLatLng == null) {
                Toast.makeText(getContext(), "Please select a location on the map", Toast.LENGTH_LONG).show();
                return;
            }

            int totalImages = imagePaths.size();
            if (totalImages == 0) {
                Toast.makeText(getContext(), "Please select at least one Image", Toast.LENGTH_SHORT).show();
                return;
            }

            saveDetailsBtn.setEnabled(false);
            Toast.makeText(getContext(), "Uploading images...", Toast.LENGTH_SHORT).show();

            List<String> uploadedImageUrls = new ArrayList<>();

            for (String path : imagePaths) {
                uploadImageToCloudinary(path, new ImageUploadCallback() {
                    @Override
                    public void onUploadSuccess(String imageUrl) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            uploadedImageUrls.add(imageUrl);
                            if (uploadedImageUrls.size() == totalImages) {
                                addToFirestore(title, address, city,
                                        selectedLatLng.latitude, selectedLatLng.longitude,
                                        rentAmount, uploadedImageUrls);
                                clearDraft();
                                titleEditText.setText("");
                                rentAmountEditText.setText("");
                                addressEditText.setText("");
                                imageContainer.removeAllViews();
                                saveDetailsBtn.setEnabled(true);
                            }
                        });
                    }

                    @Override
                    public void onUploadFailure(String error) {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(getContext(), "Image upload failed: " + error, Toast.LENGTH_SHORT).show();
                            saveDetailsBtn.setEnabled(true);
                        });
                    }
                });
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        saveDraft();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    private void saveDraft() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("title", titleEditText.getText().toString());
        editor.putString("rent", rentAmountEditText.getText().toString());
        editor.putString("address", addressEditText.getText().toString());
        editor.putInt("city_position", citySpinner.getSelectedItemPosition());
        if (selectedLatLng != null) {
            editor.putFloat("lat", (float) selectedLatLng.latitude);
            editor.putFloat("lng", (float) selectedLatLng.longitude);
        }
        Gson gson = new Gson();
        editor.putString("image_paths", gson.toJson(imagePaths));
        editor.apply();
    }

    private void loadDraft() {
        if (getView() == null) return;

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);

        if (titleEditText != null)
            titleEditText.setText(sharedPreferences.getString("title", ""));
        if (rentAmountEditText != null)
            rentAmountEditText.setText(sharedPreferences.getString("rent", ""));
        if (addressEditText != null)
            addressEditText.setText(sharedPreferences.getString("address", ""));

        citySpinner.setSelection(sharedPreferences.getInt("city_position", 0));

        if (sharedPreferences.contains("lat") && sharedPreferences.contains("lng")) {
            double lat = sharedPreferences.getFloat("lat", 0);
            double lng = sharedPreferences.getFloat("lng", 0);
            selectedLatLng = new LatLng(lat, lng);
        }

        String json = sharedPreferences.getString("image_paths", null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<String>>() {}.getType();
            imagePaths = gson.fromJson(json, type);
            for (String path : imagePaths) {
                addImageToContainer(Uri.parse(path));
            }
        }
    }

    private void clearDraft() {
        SharedPreferences sharedPreferences = getContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("title");
        editor.remove("rent");
        editor.remove("address");
        editor.remove("city_position");
        editor.remove("lat");
        editor.remove("lng");
        editor.remove("image_paths");
        editor.apply();
        imagePaths.clear();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (!isAdded()) return;
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bitmap photo = (Bitmap) data.getExtras().get("data");
                String imagePath = saveImageToInternalStorage(photo);
                imagePaths.add(imagePath);
                addImageToContainer(Uri.parse(imagePath));
            } else if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                Uri imageUri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                    String imagePath = saveImageToInternalStorage(bitmap);
                    imagePaths.add(imagePath);
                    addImageToContainer(Uri.parse(imagePath));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String saveImageToInternalStorage(Bitmap bitmap) {
        Context context = getContext();
        if (context == null) return null;
        String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
        try {
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.close();
            return context.getFilesDir().getAbsolutePath() + "/" + fileName;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void addImageToContainer(Uri imageUri) {
        LinearLayout imageContainer = requireView().findViewById(R.id.imageContainer);
        int sizeInPx = dpToPx(100);

        CardView cardView = new CardView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(sizeInPx, sizeInPx);
        params.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        cardView.setLayoutParams(params);
        cardView.setRadius(dpToPx(8));

        ImageView imageView = new ImageView(requireContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageURI(imageUri);

        cardView.setTag(imageUri.toString());
        cardView.addView(imageView);
        imageContainer.addView(cardView);

        setDoubleClickListener(cardView, imageContainer);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void deleteImageFromStorage(String path) {
        if (path == null) return;
        File file = new File(path);
        if (file.exists()) file.delete();
    }

    private void setDoubleClickListener(CardView cardView, LinearLayout imageContainer) {
        final long DOUBLE_CLICK_TIME_DELTA = 300;
        final long[] lastClickTime = {0};

        cardView.setOnClickListener(v -> {
            long clickTime = System.currentTimeMillis();
            if (clickTime - lastClickTime[0] < DOUBLE_CLICK_TIME_DELTA) {
                imageContainer.removeView(cardView);
                String imagePath = (String) cardView.getTag();
                if (imagePath != null) {
                    imagePaths.remove(imagePath);
                    deleteImageFromStorage(imagePath);
                    saveDraft();
                }
            }
            lastClickTime[0] = clickTime;
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;
            if (acceleration > SHAKE_THRESHOLD) {
                if (!isShaking) {
                    isShaking = true;
                    clearFields();
                    Toast.makeText(getContext(), "Shake detected! Fields cleared.", Toast.LENGTH_SHORT).show();
                }
            } else {
                isShaking = false;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    private void clearFields() {
        titleEditText.setText("");
        rentAmountEditText.setText("");
        addressEditText.setText("");
        citySpinner.setSelection(0);
        imagePaths.clear();
        if (imageContainer != null) {
            imageContainer.removeAllViews();
        } else {
            Log.e("clearFields", "imageContainer is null!");
        }
        Toast.makeText(getContext(), "Fields cleared due to shake gesture", Toast.LENGTH_SHORT).show();
    }

    // ─── Cloudinary Upload ───────────────────────────────────────────────────

    public interface ImageUploadCallback {
        void onUploadSuccess(String imageUrl);
        void onUploadFailure(String error);
    }

    private void uploadImageToCloudinary(String imagePath, ImageUploadCallback callback) {
        File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
            callback.onUploadFailure("Image file not found");
            return;
        }

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", imageFile.getName(),
                                RequestBody.create(MediaType.parse("image/jpeg"), imageFile))
                        .addFormDataPart("upload_preset", CLOUDINARY_UPLOAD_PRESET)
                        .build();

                Request request = new Request.Builder()
                        .url(CLOUDINARY_UPLOAD_URL)
                        .post(requestBody)
                        .build();

                Response response = client.newCall(request).execute();

                if (!response.isSuccessful()) {
                    callback.onUploadFailure("Upload failed: " + response.message());
                    return;
                }

                JSONObject json = new JSONObject(response.body().string());
                String imageUrl = json.getString("secure_url");
                callback.onUploadSuccess(imageUrl);

            } catch (IOException | JSONException e) {
                e.printStackTrace();
                callback.onUploadFailure(e.getMessage());
            }
        }).start();
    }

    // ─── Save to Firestore ───────────────────────────────────────────────────

    private void addToFirestore(String title, String address, String city,
                                double latitude, double longitude,
                                double rentAmount, List<String> imageUrls) {

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "No user is logged in.", Toast.LENGTH_SHORT).show();
            return;
        }

        Property property = new Property();
        property.setTitle(title);
        property.setAddress(address);
        property.setCity(city);
        property.setLatitude(latitude);
        property.setLongitude(longitude);
        property.setRentAmount(rentAmount);
        property.setImageUrls(imageUrls);
        property.setStatus("available");
        property.setLandlordId(currentUser.getUid());
        property.setTenantId(null);

        propertiesCollection.add(property)
                .addOnSuccessListener(documentReference ->
                        Toast.makeText(getContext(), "Property added successfully!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Error adding property: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}