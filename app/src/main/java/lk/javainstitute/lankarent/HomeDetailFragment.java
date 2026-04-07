package lk.javainstitute.lankarent;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import model.Property;
import model.User;


public class HomeDetailFragment extends Fragment {

    private ProgressBar progressBar;
    private String homeId;

    private Property property;
    private TextView title_Text,cityText,rentAmountText,landlordNameText,landlordEmailText,landlordPhoneText;
    private ViewPager2 imageViewPager;

    public HomeDetailFragment() {

    }

    public static HomeDetailFragment newInstance(String homeId){
        HomeDetailFragment fragment = new HomeDetailFragment();
        Bundle args = new Bundle();
        args.putString("homeId",homeId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(getArguments() != null){
            homeId = getArguments().getString("homeId");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home_detail, container, false);

        title_Text = view.findViewById(R.id.title_home_text);
        cityText = view.findViewById(R.id.city_text);
        rentAmountText = view.findViewById(R.id.rentAmount_text);
        landlordEmailText = view.findViewById(R.id.landlordEmail_text);
        landlordNameText = view.findViewById(R.id.landlordName_text);
        landlordPhoneText = view.findViewById(R.id.landlordPhone_text);
        imageViewPager = view.findViewById(R.id.imageViewPager);
        progressBar = view.findViewById(R.id.progressBar);

        Button payRent = view.findViewById(R.id.buttonRentPay);
        payRent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPaymentConfirmationDialog();
            }
        });

        getHomeDetails();

        MaterialButton buttonOpenMap  = view.findViewById(R.id.buttonOpenMap);
        buttonOpenMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (property != null) {
                    RouteMapDialogFragment routeMapDialogFragment = new RouteMapDialogFragment();
                    Bundle args = new Bundle();
                    args.putDouble("latitude", property.getLatitude());
                    args.putDouble("longitude", property.getLongitude());
                    routeMapDialogFragment.setArguments(args);
                    routeMapDialogFragment.show(getChildFragmentManager(), "RouteMapDialogFragment");
                }
            }
        });


        // Inflate the layout for this fragment
        return view;
    }



    private  void  getHomeDetails(){

        progressBar.setVisibility(View.VISIBLE);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("properties").document(homeId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Property property = documentSnapshot.toObject(Property.class);
                    if(property != null){
                        this.property = property;
                        title_Text.setText(property.getTitle());
                        cityText.setText(property.getCity());
                        rentAmountText.setText("Rs."+property.getRentAmount());

                        getLandlordDetails(property.getLandlordId());

                        if(property.getImageUrls() != null && !property.getImageUrls().isEmpty()){
                            ImagesSliderAdapter adapter = new ImagesSliderAdapter(getContext(), property.getImageUrls());
                            imageViewPager.setAdapter(adapter);
                            imageViewPager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
                        }
                        progressBar.setVisibility(View.GONE);

                    }
                }).addOnFailureListener(e -> Toast.makeText(getContext(),"Failed to get Home details",Toast.LENGTH_SHORT).show());
    }

    private void getLandlordDetails(String landlordId){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(landlordId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if(documentSnapshot.exists()){
                        User user = documentSnapshot.toObject(User.class);
                        if(user != null){
                            landlordNameText.setText(user.getName());
                            landlordEmailText.setText(user.getEmail());
                            landlordPhoneText.setText(user.getPhone());
                        }
                    }
                    progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Failed to get Landlord Details", Toast.LENGTH_SHORT).show();
                });
    }

    private void showPaymentConfirmationDialog() {
        if (property != null) {

            String dueMonth = getMonthName(property.getRentStartMonth()) + " " + property.getRentStartYear();

            PaymentConfirmationDialog dialog = PaymentConfirmationDialog.newInstance(
                    property.getTitle(),
                    property.getCity(),
                    "Rs." + property.getRentAmount(),
                    landlordNameText.getText().toString(),
                    landlordEmailText.getText().toString(),
                    landlordPhoneText.getText().toString(),
                    property.getLandlordId(),
                    dueMonth


            );
            dialog.show(getChildFragmentManager(), "PaymentConfirmationDialog");
        }
    }

    private String getMonthName(int month) {
        String[] months = {
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        };
        return (month >= 1 && month <= 12) ? months[month - 1] : "Unknown";
    }
}

class ImagesSliderAdapter extends RecyclerView.Adapter<ImagesSliderAdapter.ImageViewHolder>{

   static class ImageViewHolder extends RecyclerView.ViewHolder{

        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.sliderImage);
        }
    }

    private Context context;
    private List<String> imagesUrl;


    public ImagesSliderAdapter(Context context, List<String> imagesUrl) {
        this.context = context;
        this.imagesUrl = imagesUrl;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_slider, parent, false);
        view.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {

        Glide.with(context)
                .load(imagesUrl.get(position))
                .placeholder(R.drawable.add)
                .error(R.drawable.ic_home_marker)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return imagesUrl.size();
    }
}

