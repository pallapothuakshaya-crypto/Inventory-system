package com.ims.inventory.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ims.inventory.model.User;
import com.ims.inventory.service.HistoryService;
import com.ims.inventory.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final HistoryService historyService;

    public AuthController(UserService userService, HistoryService historyService) {
        this.userService = userService;
        this.historyService = historyService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");
        // Registration is strictly for Customer / User accounts. The system allows only 1 fixed Admin.
        String role = "USER";
        
        // --- NEW PASSWORD VALIDATION ---
        if (!isValidPassword(password)) {
            return ResponseEntity.badRequest().body(Map.of("error", 
                "Password must be at least 8 characters long, contain at least one uppercase letter, and at least one number."));
        }
        // --------------------------------

        try {
            User user = userService.register(username, password, role);
            historyService.log("REGISTER", "USER", user.getUsername(), "Registered new customer account", user.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<User> login(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String username = request.get("username");
        String password = request.get("password");
        
        User user = userService.login(username, password);
        
        HttpSession session = httpRequest.getSession(true);
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("role", user.getRole());
        
        historyService.log("LOGIN", "USER", user.getUsername(), "Logged into system with role: " + user.getRole(), user.getUsername());

        return ResponseEntity.ok(user);
    }

    @GetMapping("/current")
    public ResponseEntity<?> currentUser(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null && session.getAttribute("username") != null) {
            return ResponseEntity.ok(Map.of(
                "userId", session.getAttribute("userId"),
                "username", session.getAttribute("username"),
                "role", session.getAttribute("role") != null ? session.getAttribute("role") : "USER"
            ));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            String username = (String) session.getAttribute("username");
            if (username != null) {
                historyService.log("LOGOUT", "USER", username, "User logged out", username);
            }
            session.invalidate();
        }
        return ResponseEntity.ok("Logged out successfully");
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable long id, HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        String performedBy = (session != null && session.getAttribute("username") != null) 
            ? (String) session.getAttribute("username") : "Admin";
        
        userService.deleteUser(id);
        historyService.log("DELETE", "USER", "User #" + id, "Admin deleted user account #" + id, performedBy);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> req, HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        String username = (session != null && session.getAttribute("username") != null) 
            ? (String) session.getAttribute("username") : req.get("username");
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        try {
            userService.changePassword(username, req.get("oldPassword"), req.get("newPassword"));
            historyService.log("UPDATE", "USER", username, "Changed account password", username);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Password changed successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> req) {
        try {
            String username = req.get("username");
            String newPassword = req.get("newPassword");
            
            // Add this check
            if (!isValidPassword(newPassword)) {
                return ResponseEntity.badRequest().body(Map.of("error", 
                    "Password must be at least 8 characters, contain 1 uppercase letter, and 1 number."));
            }
            
            userService.resetPassword(username, newPassword);
            historyService.log("RESET_PWD", "USER", username, "Password reset for user", username);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Password reset successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> req, HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        String username = (session != null && session.getAttribute("username") != null) 
            ? (String) session.getAttribute("username") : req.get("username");
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Not authenticated"));
        }
        try {
            userService.updateProfile(username, req.get("email"), req.get("phone"));
            historyService.log("UPDATE", "USER", username, "Updated profile info", username);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Profile updated successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable long id, HttpServletRequest httpRequest) {
        try {
            userService.toggleUserStatus(id);
            return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "User status updated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private boolean isValidPassword(String password) {
        if (password == null) return false;
        // Regex: (?=.*[A-Z]) = at least 1 uppercase, (?=.*\d) = at least 1 number, .{8,} = at least 8 characters
        String passwordPattern = "^(?=.*[A-Z])(?=.*\\d).{8,}$";
        return password.matches(passwordPattern);
    }
}