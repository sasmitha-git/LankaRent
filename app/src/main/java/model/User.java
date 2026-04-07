package model;

public class User {

    private String name;
    private String phone;
    private  String email;
    private String password;
    private String role;

    private String fcmToken;

    public User() {
    }

    public User(String name, String phone, String email, String role,String fcmToken) {
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.role = role;
        this.fcmToken = fcmToken;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}
