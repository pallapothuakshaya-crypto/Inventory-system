package com.ims.inventory.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.ims.inventory.config.DatabaseConnection;
import com.ims.inventory.model.Purchase;

@Repository
public class PurchaseDAOImpl implements PurchaseDAO {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public Purchase createPurchase(Purchase purchase) {
        String sql = "INSERT INTO purchases (po_number, supplier_id, supplier_name, product_id, product_name, quantity, unit_cost, total_cost, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, purchase.getPoNumber());
            ps.setLong(2, purchase.getSupplierId());
            ps.setString(3, purchase.getSupplierName());
            ps.setLong(4, purchase.getProductId());
            ps.setString(5, purchase.getProductName());
            ps.setInt(6, purchase.getQuantity());
            ps.setBigDecimal(7, purchase.getUnitCost());
            ps.setBigDecimal(8, purchase.getTotalCost());
            ps.setString(9, purchase.getStatus() != null ? purchase.getStatus() : "RECEIVED");
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    purchase.setPurchaseId(keys.getLong(1));
                }
            }
            purchase.setCreatedAt(LocalDateTime.now());
            return purchase;
        } catch (SQLException e) {
            throw new DaoException("Could not create purchase: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Purchase> getAllPurchases() {
        String sql = "SELECT * FROM purchases ORDER BY purchase_id DESC";
        List<Purchase> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Could not fetch purchases: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Purchase> getPurchaseById(long purchaseId) {
        String sql = "SELECT * FROM purchases WHERE purchase_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, purchaseId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Could not fetch purchase: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updatePurchaseStatus(long purchaseId, String status) {
        String sql = "UPDATE purchases SET status = ? WHERE purchase_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, purchaseId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Could not update purchase status: " + e.getMessage(), e);
        }
    }

    private Purchase mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime created = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        return new Purchase(
            rs.getLong("purchase_id"),
            rs.getString("po_number"),
            rs.getLong("supplier_id"),
            rs.getString("supplier_name"),
            rs.getLong("product_id"),
            rs.getString("product_name"),
            rs.getInt("quantity"),
            rs.getBigDecimal("unit_cost"),
            rs.getBigDecimal("total_cost"),
            rs.getString("status"),
            created
        );
    }
}
