package lk.javainstitute.lankarent;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class AddImageBottomSheet extends BottomSheetDialogFragment {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;

    private boolean isLocationInstruction = false;  // Flag to check if it's for location instructions or image selection

    public void setLocationInstructionFlag(boolean isLocationInstruction) {
        this.isLocationInstruction = isLocationInstruction;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_add_image, container, false);

        if (isLocationInstruction) {
            // Show location instruction UI
            showLocationInstructions(view);
        } else {
            // Show image selection UI
            showImageOptions(view);
        }

        return view;
    }

    private void showImageOptions(View view) {
        Button openCameraButton = view.findViewById(R.id.open_camera_button);
        Button selectFromGalleryButton = view.findViewById(R.id.select_from_gallery_button);

        openCameraButton.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            requireParentFragment().startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            dismiss();
        });

        selectFromGalleryButton.setOnClickListener(v -> {
            Intent pickPhoto = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            requireParentFragment().startActivityForResult(pickPhoto, REQUEST_IMAGE_PICK);
            dismiss();
        });
    }

    private void showLocationInstructions(View view) {
        // Hide image selection buttons
        Button openCameraButton = view.findViewById(R.id.open_camera_button);
        Button selectFromGalleryButton = view.findViewById(R.id.select_from_gallery_button);
        openCameraButton.setVisibility(View.GONE);
        selectFromGalleryButton.setVisibility(View.GONE);

        // Show instruction text
        TextView instructionTextView = view.findViewById(R.id.instructionTextView);
        instructionTextView.setVisibility(View.VISIBLE);
        instructionTextView.setText("Before you select your location on Google Maps, please add your home to Google Maps.\n\n"
                + "To do this, go to Google Maps, click 'Add Home', and set a name for your location. Then, come back here and select the location.");
    }
}
