package com.ims.inventory.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.ims.inventory.config.DatabaseConnection;
import com.ims.inventory.model.Supplier;
import com.ims.inventory.model.SupplierProduct;

@Repository
public class SupplierDAOImpl implements SupplierDAO {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public void addSupplier(Supplier s) {
        String sql = "INSERT INTO suppliers (supplier_name, contact_person, phone, email, address) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getSupplierName());
            ps.setString(2, s.getContactPerson());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getEmail());
            ps.setString(5, s.getAddress());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) s.setSupplierId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new DaoException("Could not add supplier", e);
        }
    }

    @Override
    public boolean updateSupplier(Supplier s) {
        String sql = "UPDATE suppliers SET supplier_name=?, contact_person=?, phone=?, email=?, address=? WHERE supplier_id=?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, s.getSupplierName());
            ps.setString(2, s.getContactPerson());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getEmail());
            ps.setString(5, s.getAddress());
            ps.setLong(6, s.getSupplierId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Could not update supplier", e);
        }
    }

    @Override
    public boolean deleteSupplier(long id) {
        String sql = "DELETE FROM suppliers WHERE supplier_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Could not delete supplier", e);
        }
    }

    @Override
    public Optional<Supplier> getSupplierById(long id) {
        String sql = "SELECT * FROM suppliers WHERE supplier_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Could not read supplier", e);
        }
    }

    @Override
    public List<Supplier> getAllSuppliers() {
        String sql = "SELECT * FROM suppliers ORDER BY supplier_name";
        List<Supplier> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            throw new DaoException("Could not list suppliers", e);
        }
    }

    @Override
    public void linkProductToSupplier(SupplierProduct sp) {
        String sql = "INSERT INTO supplier_products (supplier_id, product_id, supply_price, lead_time) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, sp.getSupplierId());
            ps.setLong(2, sp.getProductId());
            ps.setBigDecimal(3, sp.getSupplyPrice());
            ps.setInt(4, sp.getLeadTime());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) sp.setSupplierProductId(keys.getLong(1));
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateEntryException("This supplier is already linked to this product", e);
        } catch (SQLException e) {
            throw new DaoException("Could not link product to supplier", e);
        }
    }

    @Override
    public List<SupplierProduct> getProductsBySupplier(long supplierId) {
        String sql = "SELECT * FROM supplier_products WHERE supplier_id = ?";
        List<SupplierProduct> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, supplierId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new SupplierProduct(
                        rs.getLong("supplier_product_id"),
                        rs.getLong("supplier_id"),
                        rs.getLong("product_id"),
                        rs.getBigDecimal("supply_price"),
                        rs.getInt("lead_time")
                    ));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Could not list supplier products", e);
        }
    }

    @Override
    public boolean isSupplierInUse(long id) {
        String sql = "SELECT COUNT(*) FROM supplier_products WHERE supplier_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DaoException("Could not check supplier usage", e);
        }
    }

    private Supplier mapRow(ResultSet rs) throws SQLException {
        return new Supplier(
            rs.getLong("supplier_id"),
            rs.getString("supplier_name"),
            rs.getString("contact_person"),
            rs.getString("phone"),
            rs.getString("email"),
            rs.getString("address")
        );
    }
}