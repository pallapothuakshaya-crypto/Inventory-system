package com.ims.inventory.model;

public class User {
    private Long userId;
    private String username;
    private String password; // This will store the hashed password
    private String role;
    private String email;
    private String phone;
    private String status;

    public User() { 
        this.status = "ACTIVE";
    }

    public User(Long userId, String username, String password, String role) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.role = role;
        this.status = "ACTIVE";
    }

    public User(Long userId, String username, String password, String role, String email, String phone, String status) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.role = role;
        this.email = email;
        this.phone = phone;
        this.status = status != null ? status : "ACTIVE";
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status != null ? status : "ACTIVE"; }
    public void setStatus(String status) { this.status = status != null ? status : "ACTIVE"; }
}