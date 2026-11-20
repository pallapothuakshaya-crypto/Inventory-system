package com.ims.inventory.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.ims.inventory.config.DatabaseConnection;
import com.ims.inventory.model.User;

@Repository
public class UserDAOImpl implements UserDAO {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public void saveUser(User user) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRole());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) user.setUserId(keys.getLong(1));
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062 || (e.getMessage() != null && e.getMessage().toLowerCase().contains("duplicate"))) {
                throw new DuplicateEntryException("Username '" + user.getUsername() + "' is already taken", e);
            }
            throw new DaoException("Could not save user: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String email = null;
                    try { email = rs.getString("email"); } catch (SQLException ignored) {}
                    String phone = null;
                    try { phone = rs.getString("phone"); } catch (SQLException ignored) {}
                    String status = "ACTIVE";
                    try { 
                        String s = rs.getString("status"); 
                        if (s != null) status = s;
                    } catch (SQLException ignored) {}

                    return Optional.of(new User(
                        rs.getLong("user_id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        email,
                        phone,
                        status
                    ));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Could not find user: " + e.getMessage(), e);
        }
    }

    @Override
    public List<User> getAllUsers() {
        String sql = "SELECT * FROM users ORDER BY user_id ASC";
        List<User> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String email = null;
                try { email = rs.getString("email"); } catch (SQLException ignored) {}
                String phone = null;
                try { phone = rs.getString("phone"); } catch (SQLException ignored) {}
                String status = "ACTIVE";
                try { 
                    String s = rs.getString("status"); 
                    if (s != null) status = s;
                } catch (SQLException ignored) {}

                list.add(new User(
                    rs.getLong("user_id"),
                    rs.getString("username"),
                    null,
                    rs.getString("role"),
                    email,
                    phone,
                    status
                ));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Could not fetch users: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteUser(long userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Could not delete user: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updatePassword(String username, String newHashedPassword) {
        String sql = "UPDATE users SET password = ? WHERE username = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, newHashedPassword);
            ps.setString(2, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Could not update password", e);
        }
    }

    @Override
    public boolean updateUserStatus(long userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE user_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Could not update user status", e);
        }
    }

    @Override
    public boolean updateUserProfile(String username, String email, String phone) {
        String sql = "UPDATE users SET email = ?, phone = ? WHERE username = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, phone);
            ps.setString(3, username);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Could not update user profile", e);
        }
    }
}