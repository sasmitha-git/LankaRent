package lk.javainstitute.lankarent;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapDialogFragment extends DialogFragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLatLng;
    private String selectedCity;

    // Callback interface to pass the selected location back to the parent fragment
    public interface OnLocationSelectedListener {
        void onLocationSelected(LatLng latLng);
    }

    private OnLocationSelectedListener locationSelectedListener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Use getParentFragment() to get the callback from AddPropertyFragment
        if (getParentFragment() instanceof OnLocationSelectedListener) {
            locationSelectedListener = (OnLocationSelectedListener) getParentFragment();
        } else {
            throw new RuntimeException("Parent fragment must implement OnLocationSelectedListener");
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);  // Make background transparent if needed
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Retrieve the selected city from arguments
        if (getArguments() != null) {
            selectedCity = getArguments().getString("city_name", "Sri Lanka");
        }

        // Initialize the map
        SupportMapFragment supportMapFragment = new SupportMapFragment();
        getChildFragmentManager().beginTransaction().replace(R.id.mapLayout, supportMapFragment).commit();
        supportMapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Get the coordinates of the selected city
        LatLng cityLatLng = getCityCoordinates(selectedCity);

        // Move the camera to the selected city with a suitable zoom level
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cityLatLng, 12.0f));

        // Enable zoom controls and gestures
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        // Set up long press listener for selecting a location
        mMap.setOnMapLongClickListener(latLng -> {
            mMap.clear();  // Clear previous markers

            // Add marker with home icon at the selected location
            mMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title("Selected Location")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_home_marker)));

            selectedLatLng = latLng;  // Save selected coordinates

            // Notify the listener with the selected location
            if (locationSelectedListener != null) {
                locationSelectedListener.onLocationSelected(latLng);
            }

            // Save the location to SharedPreferences
            if (getContext() != null) {
                SharedPreferences sharedPreferences = getContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putFloat("lat", (float) latLng.latitude);
                editor.putFloat("lng", (float) latLng.longitude);
                editor.apply();
            }

            Toast.makeText(getContext(), "Location Set", Toast.LENGTH_LONG).show();

            Log.e("app", "location: " + selectedLatLng);

            // Dismiss the dialog after 1 second
            new Handler(Looper.getMainLooper()).postDelayed(this::dismiss, 1000);
        });
    }

    // Helper method to get coordinates for a given city
    private LatLng getCityCoordinates(String city) {
        switch (city) {
            case "Colombo":
                return new LatLng(6.9271, 79.8612);
            case "Kandy":
                return new LatLng(7.2906, 80.6337);
            case "Galle":
                return new LatLng(6.0535, 80.2210);
            case "Jaffna":
                return new LatLng(9.6615, 80.0255);
            case "Matara":
                return new LatLng(5.9485, 80.5353);
            case "Ratnapura":
                return new LatLng(6.6828, 80.3994);
            default:
                return new LatLng(7.8731, 80.7718); // Default to Sri Lanka
        }
    }
}