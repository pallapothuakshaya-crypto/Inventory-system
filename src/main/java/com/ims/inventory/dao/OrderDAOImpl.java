package com.ims.inventory.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.ims.inventory.config.DatabaseConnection;
import com.ims.inventory.model.Order;

@Repository
public class OrderDAOImpl implements OrderDAO {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public Order createOrder(Order order) {
        String sql = "INSERT INTO orders (order_number, customer_username, product_id, product_name, quantity, unit_price, total_amount, status, payment_status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, order.getOrderNumber());
            ps.setString(2, order.getCustomerUsername());
            ps.setLong(3, order.getProductId());
            ps.setString(4, order.getProductName());
            ps.setInt(5, order.getQuantity());
            ps.setBigDecimal(6, order.getUnitPrice());
            ps.setBigDecimal(7, order.getTotalAmount());
            ps.setString(8, order.getStatus() != null ? order.getStatus() : "COMPLETED");
            ps.setString(9, order.getPaymentStatus() != null ? order.getPaymentStatus() : "PAID");
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    order.setOrderId(keys.getLong(1));
                }
            }
            order.setCreatedAt(LocalDateTime.now());
            return order;
        } catch (SQLException e) {
            throw new DaoException("Could not save order: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Order> getAllOrders() {
        String sql = "SELECT * FROM orders ORDER BY order_id DESC";
        List<Order> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Could not fetch orders: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Order> getOrdersByCustomer(String customerUsername) {
        String sql = "SELECT * FROM orders WHERE customer_username = ? ORDER BY order_id DESC";
        List<Order> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, customerUsername);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Could not fetch customer orders: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Order> getOrderById(long orderId) {
        String sql = "SELECT * FROM orders WHERE order_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Could not fetch order: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean cancelOrder(long orderId) {
        String sql = "UPDATE orders SET status = 'CANCELLED' WHERE order_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, orderId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Could not cancel order: " + e.getMessage(), e);
        }
    }

    private Order mapRow(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("created_at");
        LocalDateTime created = ts != null ? ts.toLocalDateTime() : LocalDateTime.now();

        return new Order(
            rs.getLong("order_id"),
            rs.getString("order_number"),
            rs.getString("customer_username"),
            rs.getLong("product_id"),
            rs.getString("product_name"),
            rs.getInt("quantity"),
            rs.getBigDecimal("unit_price"),
            rs.getBigDecimal("total_amount"),
            rs.getString("status"),
            rs.getString("payment_status"),
            created
        );
    }
}
