package com.example.mangocam;

import com.google.firebase.firestore.Exclude;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class HelperClass implements Serializable {

    private String userId, name, email, contact, address, birthday, password, dateJoined;

    public HelperClass() {} // Firestore needs empty constructor

    public HelperClass(String userId, String name, String email, String contact,
                       String address, int mangoTrees, String birthday, String password, String dateJoined) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.contact = contact;
        this.address = address;
        this.birthday = birthday;
        this.password = password;
        this.dateJoined = dateJoined;
    }

    // Getters / Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getDateJoined() { return dateJoined; }
    public void setDateJoined(String dateJoined) { this.dateJoined = dateJoined; }

    @Exclude
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("name", name);
        map.put("email", email);
        map.put("contact", contact);
        map.put("address", address);
        map.put("birthday", birthday);
        map.put("password", password);
        map.put("dateJoined", dateJoined);
        return map;
    }
}
