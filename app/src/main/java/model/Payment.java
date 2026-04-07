package model;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Payment {

    private String tenantId;

    private String landlordId;
    private String propertyId;
    private double amount;
    private int month;
    private int year;
    @ServerTimestamp
    private Date timeStamp;
    private Date dueDate;
    private boolean notified;
    public Payment() {
    }

    public Payment(String tenantId,String landlordId, String propertyId, double amount, int month, int year, Date timeStamp,Date dueDate,boolean notified) {
        this.tenantId = tenantId;
        this.landlordId = landlordId;
        this.propertyId = propertyId;
        this.amount = amount;
        this.month = month;
        this.year = year;
        this.timeStamp = timeStamp;
        this.dueDate = dueDate;
        this.notified = notified;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getLandlordId() {
        return landlordId;
    }

    public void setLandlordId(String landlordId) {
        this.landlordId = landlordId;
    }

    public String getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(String propertyId) {
        this.propertyId = propertyId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public boolean isNotified() {
        return notified;
    }

    public void setNotified(boolean notified) {
        this.notified = notified;
    }
}
