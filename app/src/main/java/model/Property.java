package model;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;
import java.util.List;

@IgnoreExtraProperties
public class Property {

    @DocumentId
    private String propertyId;
    private String title;
    private String address;

    private String city;
    private double latitude;
    private double longitude;
    private double rentAmount;
    private List<String> imageUrls;
    private String status;

    private String landlordId;
    private String tenantId;

    private  Integer rentStartMonth;
    private  Integer rentStartYear;
    public Property() {

    }



    public Property(String title, String address, String city, double latitude, double longitude, double rentAmount, List<String> imageUrls, String status, String landlordId, String tenantId) {
        this.title = title;
        this.address = address;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.rentAmount = rentAmount;
        this.imageUrls = imageUrls;
        this.status = status;
        this.landlordId = landlordId;
        this.tenantId = tenantId;
        this.rentStartMonth = null;
        this.rentStartYear = null;
    }

    // Getters and setters for each field

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getRentAmount() {
        return rentAmount;
    }

    public void setRentAmount(double rentAmount) {
        this.rentAmount = rentAmount;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLandlordId() {
        return landlordId;
    }

    public void setLandlordId(String landlordId) {
        this.landlordId = landlordId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public int getRentStartMonth() {
        return rentStartMonth != null ? rentStartMonth : 0;
    }

    public void setRentStartMonth(int rentStartMonth) {
        this.rentStartMonth = rentStartMonth;
    }

    public int getRentStartYear() {
        return rentStartYear != null ? rentStartYear : 0;
    }

    public void setRentStartYear(int rentStartYear) {
        this.rentStartYear = rentStartYear;
    }
}
