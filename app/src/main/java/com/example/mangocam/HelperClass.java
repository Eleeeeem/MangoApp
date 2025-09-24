package com.example.mangocam;

public class HelperClass {
    private String userId;
    private String name;
    private String email;
    private String contact;
    private String address;
    private int mangoTrees;
    private String birthday; // Add this line
    private String password;

    // Default constructor (required for Firebase)
    public HelperClass() {}

    // Parameterized Constructor
    public HelperClass(String userId, String name, String email, String contact, String address, int mangoTrees, String birthday, String password) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.contact = contact;
        this.address = address;
        this.mangoTrees = mangoTrees;
        this.birthday = birthday; // Initialize the new field
        this.password = password;
    }

    // Getters and Setters
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

    public int getMangoTrees() { return mangoTrees; }
    public void setMangoTrees(int mangoTrees) { this.mangoTrees = mangoTrees; }

    public String getBirthday() { return birthday; } // New getter
    public void setBirthday(String birthday) { this.birthday = birthday; } // New setter

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}