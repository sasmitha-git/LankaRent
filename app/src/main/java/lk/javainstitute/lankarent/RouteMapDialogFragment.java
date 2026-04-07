package lk.javainstitute.lankarent;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

public class RouteMapDialogFragment extends DialogFragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng homeLatLng; // Home location coordinates
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<String[]> locationPermissionRequest;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize the ActivityResultLauncher for location permissions
        locationPermissionRequest = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    Boolean fineLocationGranted = result.get(Manifest.permission.ACCESS_FINE_LOCATION);
                    Boolean coarseLocationGranted = result.get(Manifest.permission.ACCESS_COARSE_LOCATION);

                    if (fineLocationGranted != null && fineLocationGranted) {
                        // Fine location permission granted, get the location
                        getCurrentLocation();
                    } else if (coarseLocationGranted != null && coarseLocationGranted) {
                        // Coarse location permission granted, get the location
                        getCurrentLocation();
                    } else {
                        // Location permission denied
                        Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent); // Make background transparent
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

        // Retrieve home location from arguments
        if (getArguments() != null) {
            double latitude = getArguments().getDouble("latitude");
            double longitude = getArguments().getDouble("longitude");
            homeLatLng = new LatLng(latitude, longitude);
        }

        // Initialize the map
        SupportMapFragment supportMapFragment = new SupportMapFragment();
        getChildFragmentManager().beginTransaction().replace(R.id.mapLayout, supportMapFragment).commit();
        supportMapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Enable zoom controls and gestures
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        // Get the user's current location
        getCurrentLocation();
    }

    private void getCurrentLocation() {
        if (fusedLocationClient == null) {
            // Initialize FusedLocationProviderClient if it's null
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        }

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions using ActivityResultLauncher
            locationPermissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(requireActivity(), location -> {
                    if (location != null) {
                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                        drawPolyline(userLatLng, homeLatLng); // Draw polyline between user and home
                        moveCameraToShowBothLocations(userLatLng, homeLatLng); // Move camera to show both locations
                    } else {
                        Toast.makeText(requireContext(), "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void drawPolyline(LatLng origin, LatLng destination) {
        // Add markers for user and home locations
        mMap.addMarker(new MarkerOptions()
                .position(origin)
                .title("Your Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));

        mMap.addMarker(new MarkerOptions()
                .position(destination)
                .title("Home Location")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_home_marker)));

        // Draw a polyline between the two points
        PolylineOptions polylineOptions = new PolylineOptions()
                .add(origin, destination)
                .width(10)
                .color(Color.RED);
        mMap.addPolyline(polylineOptions);
    }

    private void moveCameraToShowBothLocations(LatLng origin, LatLng destination) {
        // Create a LatLngBounds to include both locations
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(origin)
                .include(destination)
                .build();

        // Move the camera to show the bounds with padding
        int padding = 100; // Padding in pixels
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));
    }
}