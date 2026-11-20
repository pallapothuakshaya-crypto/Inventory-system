package com.ims.inventory.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.ims.inventory.config.DatabaseConnection;

@Repository
public class NotificationDAOImpl implements NotificationDAO {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public void saveNotification(long productId, String email) {
        String sql = "INSERT INTO restock_notifications (product_id, email, status) VALUES (?, ?, 'PENDING')";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, productId);
            ps.setString(2, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Could not save restock notification", e);
        }
    }

    @Override
    public List<String> getPendingEmails(long productId) {
        String sql = "SELECT email FROM restock_notifications WHERE product_id = ? AND status = 'PENDING'";
        List<String> emails = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    emails.add(rs.getString("email"));
                }
            }
            return emails;
        } catch (SQLException e) {
            throw new DaoException("Could not fetch pending notifications", e);
        }
    }

    @Override
    public void markAsSent(long productId, String email) {
        String sql = "UPDATE restock_notifications SET status = 'SENT' WHERE product_id = ? AND email = ? AND status = 'PENDING'";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, productId);
            ps.setString(2, email);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DaoException("Could not mark notification as sent", e);
        }
    }
}