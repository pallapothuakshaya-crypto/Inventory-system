package com.ims.inventory.service;

import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.ims.inventory.dao.DuplicateEntryException;
import com.ims.inventory.dao.UserDAO;
import com.ims.inventory.model.User;

import jakarta.annotation.PostConstruct;

@Service
public class UserService {

    private final UserDAO userDAO;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @PostConstruct
    public void initDefaultUsers() {
        try {
            if (userDAO.findByUsername("admin").isEmpty()) {
                register("admin", "admin123", "ADMIN");
                System.out.println(">>> Pre-seeded default admin in database (admin / admin123)");
            }
        } catch (Exception ignored) {}
        try {
            if (userDAO.findByUsername("user").isEmpty()) {
                register("user", "user123", "USER");
                System.out.println(">>> Pre-seeded default user in database (user / user123)");
            }
        } catch (Exception ignored) {}
    }

    public User register(String username, String rawPassword) {
        return register(username, rawPassword, "USER");
    }

    public User register(String username, String rawPassword, String role) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Username and password are required");
        }
        
        String cleanUsername = username.trim();
        if (cleanUsername.length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long");
        }
        if (rawPassword.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters long");
        }

        if (userDAO.findByUsername(cleanUsername).isPresent()) {
            throw new BusinessRuleException("Username '" + cleanUsername + "' is already taken. Please choose another.");
        }

        String assignedRole = (role != null && !role.isBlank()) ? role.trim().toUpperCase() : "USER";
        if ("ADMIN".equals(assignedRole) && !"admin".equalsIgnoreCase(cleanUsername)) {
            // Admin role is strictly restricted to the single system administrator!
            assignedRole = "USER";
        } else if (!"ADMIN".equals(assignedRole)) {
            assignedRole = "USER";
        }

        String hashedPassword = passwordEncoder.encode(rawPassword);
        User newUser = new User(null, cleanUsername, hashedPassword, assignedRole);
        
        try {
            userDAO.saveUser(newUser);
        } catch (DuplicateEntryException e) {
            throw new BusinessRuleException("Username '" + cleanUsername + "' is already taken");
        }
        
        newUser.setPassword(null);
        return newUser;
    }

    public User login(String username, String rawPassword) {
        if (username == null || rawPassword == null || username.isBlank() || rawPassword.isBlank()) {
            throw new ResourceNotFoundException("Invalid username or password");
        }

        Optional<User> userOpt = userDAO.findByUsername(username.trim());
        
        if (userOpt.isEmpty()) {
            throw new ResourceNotFoundException("Invalid username or password");
        }

        User user = userOpt.get();
        if ("INACTIVE".equalsIgnoreCase(user.getStatus()) || "DEACTIVATED".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalStateException("Your account is deactivated. Please contact the administrator.");
        }

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new ResourceNotFoundException("Invalid username or password");
        }

        user.setPassword(null);
        return user;
    }

    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }

    public void deleteUser(long userId) {
        boolean deleted = userDAO.deleteUser(userId);
        if (!deleted) {
            throw new ResourceNotFoundException("User " + userId + " not found");
        }
    }

    public boolean changePassword(String username, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.length() < 4) {
            throw new IllegalArgumentException("New password must be at least 4 characters long");
        }
        User user = userDAO.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect current password");
        }

        String hashed = passwordEncoder.encode(newPassword);
        return userDAO.updatePassword(username, hashed);
    }

    public boolean resetPassword(String username, String newPassword) {
        if (newPassword == null || newPassword.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters long");
        }
        User user = userDAO.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        String hashed = passwordEncoder.encode(newPassword);
        return userDAO.updatePassword(username, hashed);
    }

    public boolean updateProfile(String username, String email, String phone) {
        User user = userDAO.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        return userDAO.updateUserProfile(username, email, phone);
    }

    public boolean toggleUserStatus(long userId) {
        User user = userDAO.getAllUsers().stream()
            .filter(u -> u.getUserId() != null && u.getUserId() == userId)
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("User #" + userId + " not found"));

        if ("admin".equalsIgnoreCase(user.getUsername())) {
            throw new IllegalStateException("Primary admin account cannot be deactivated!");
        }

        String nextStatus = "ACTIVE".equalsIgnoreCase(user.getStatus()) ? "INACTIVE" : "ACTIVE";
        return userDAO.updateUserStatus(userId, nextStatus);
    }
}