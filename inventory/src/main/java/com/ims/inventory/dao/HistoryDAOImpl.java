package com.ims.inventory.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.ims.inventory.config.DatabaseConnection;
import com.ims.inventory.model.ActivityLog;

@Repository
public class HistoryDAOImpl implements HistoryDAO {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    private void ensureTableExists() {
        try {
            String createTableSql = "CREATE TABLE IF NOT EXISTS activity_logs (" +
                "log_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "action_type VARCHAR(50) NOT NULL, " +
                "entity_type VARCHAR(50) NOT NULL, " +
                "entity_name VARCHAR(255), " +
                "details TEXT, " +
                "performed_by VARCHAR(50) NOT NULL, " +
                "timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                ")";
            try (Statement st = conn().createStatement()) {
                st.execute(createTableSql);
            }
        } catch (SQLException ignored) {
        }
    }

    @Override
    public void logActivity(ActivityLog log) {
        ensureTableExists();
        String sql = "INSERT INTO activity_logs (action_type, entity_type, entity_name, details, performed_by) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, log.getActionType());
            ps.setString(2, log.getEntityType());
            ps.setString(3, log.getEntityName());
            ps.setString(4, log.getDetails());
            ps.setString(5, log.getPerformedBy() != null ? log.getPerformedBy() : "Anonymous");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    log.setLogId(keys.getLong(1));
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Could not record activity log", e);
        }
    }

    @Override
    public List<ActivityLog> getAllLogs() {
        ensureTableExists();
        String sql = "SELECT * FROM activity_logs ORDER BY timestamp DESC, log_id DESC LIMIT 500";
        List<ActivityLog> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Could not fetch activity logs", e);
        }
    }

    @Override
    public List<ActivityLog> getLogsByUser(String username) {
        ensureTableExists();
        String sql = "SELECT * FROM activity_logs WHERE performed_by = ? ORDER BY timestamp DESC, log_id DESC";
        List<ActivityLog> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Could not fetch user activity logs", e);
        }
    }

    @Override
    public List<ActivityLog> searchLogs(String keyword) {
        ensureTableExists();
        String sql = "SELECT * FROM activity_logs WHERE entity_name LIKE ? OR action_type LIKE ? OR performed_by LIKE ? OR details LIKE ? ORDER BY timestamp DESC, log_id DESC";
        List<ActivityLog> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            String query = "%" + keyword + "%";
            ps.setString(1, query);
            ps.setString(2, query);
            ps.setString(3, query);
            ps.setString(4, query);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Could not search activity logs", e);
        }
    }

    private ActivityLog mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("timestamp");
        return new ActivityLog(
            rs.getLong("log_id"),
            rs.getString("action_type"),
            rs.getString("entity_type"),
            rs.getString("entity_name"),
            rs.getString("details"),
            rs.getString("performed_by"),
            ts != null ? ts.toLocalDateTime() : null
        );
    }
}
