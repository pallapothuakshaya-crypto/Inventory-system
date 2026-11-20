package com.ims.inventory.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.ims.inventory.config.DatabaseConnection;
import com.ims.inventory.model.Product;

@Repository
public class ProductDAOImpl implements ProductDAO {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    private void ensureColumnsExist() {
        try {
            try (Statement st = conn().createStatement()) {
                st.execute("ALTER TABLE products ADD COLUMN image_url VARCHAR(255)");
            }
        } catch (SQLException ignored) {}
        try {
            try (Statement st = conn().createStatement()) {
                st.execute("ALTER TABLE products ADD COLUMN quantity INT NOT NULL DEFAULT 0");
            }
        } catch (SQLException ignored) {}
        try {
            try (Statement st = conn().createStatement()) {
                st.execute("ALTER TABLE products ADD COLUMN sold_quantity INT NOT NULL DEFAULT 0");
            }
        } catch (SQLException ignored) {}
    }

    @Override
    public void addProduct(Product p) {
        ensureColumnsExist();
        String sqlProduct = "INSERT INTO products (product_name, description, price, category_id, image_url, quantity, sold_quantity, min_stock, expiry_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlStock = "INSERT INTO stocks (product_id, quantity, available_quantity) VALUES (?, ?, ?)";
        
        int qty = p.getQuantity() != null ? p.getQuantity() : 0;
        int sold = p.getSoldQuantity() != null ? p.getSoldQuantity() : 0;
        int minStock = p.getMinStock() != null ? p.getMinStock() : 5;

        try (PreparedStatement ps = conn().prepareStatement(sqlProduct, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getProductName());
            ps.setString(2, p.getDescription());
            ps.setBigDecimal(3, p.getPrice());
            ps.setLong(4, p.getCategoryId());
            ps.setString(5, p.getImageUrl());
            ps.setInt(6, qty);
            ps.setInt(7, sold);
            ps.setInt(8, minStock);
            ps.setString(9, p.getExpiryDate());
            ps.executeUpdate();
            
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long newProductId = keys.getLong(1);
                    p.setProductId(newProductId);
                    try (PreparedStatement psStock = conn().prepareStatement(sqlStock)) {
                        psStock.setLong(1, newProductId);
                        psStock.setInt(2, qty + sold);
                        psStock.setInt(3, qty);
                        psStock.executeUpdate();
                    } catch (SQLException ignored) {}
                }
            }
        } catch (SQLException e) {
            throw new DaoException("Could not add product", e);
        }
    }

    @Override
    public boolean updateProduct(Product p) {
        ensureColumnsExist();
        String sql = "UPDATE products SET product_name = ?, description = ?, price = ?, category_id = ?, image_url = ?, quantity = ?, sold_quantity = ?, min_stock = ?, expiry_date = ? WHERE product_id = ?";
        String sqlStock = "UPDATE stocks SET quantity = ?, available_quantity = ?, last_updated = CURRENT_TIMESTAMP WHERE product_id = ?";
        int qty = p.getQuantity() != null ? p.getQuantity() : 0;
        int sold = p.getSoldQuantity() != null ? p.getSoldQuantity() : 0;
        int minStock = p.getMinStock() != null ? p.getMinStock() : 5;
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, p.getProductName());
            ps.setString(2, p.getDescription());
            ps.setBigDecimal(3, p.getPrice());
            ps.setLong(4, p.getCategoryId());
            ps.setString(5, p.getImageUrl());
            ps.setInt(6, qty);
            ps.setInt(7, sold);
            ps.setInt(8, minStock);
            ps.setString(9, p.getExpiryDate());
            ps.setLong(10, p.getProductId());
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                try (PreparedStatement psStock = conn().prepareStatement(sqlStock)) {
                    psStock.setInt(1, qty + sold);
                    psStock.setInt(2, qty);
                    psStock.setLong(3, p.getProductId());
                    psStock.executeUpdate();
                } catch (SQLException ignored) {}
            }
            return updated;
        } catch (SQLException e) {
            throw new DaoException("Could not update product", e);
        }
    }

    @Override
    public boolean selectOrBuyProduct(long id, int count) {
        ensureColumnsExist();
        String updateProductSql = "UPDATE products SET quantity = quantity - ?, sold_quantity = sold_quantity + ? WHERE product_id = ? AND quantity >= ?";
        String updateStockSql = "UPDATE stocks SET available_quantity = available_quantity - ?, last_updated = CURRENT_TIMESTAMP WHERE product_id = ? AND available_quantity >= ?";
        try (PreparedStatement ps = conn().prepareStatement(updateProductSql)) {
            ps.setInt(1, count);
            ps.setInt(2, count);
            ps.setLong(3, id);
            ps.setInt(4, count);
            int affected = ps.executeUpdate();
            if (affected > 0) {
                try (PreparedStatement psStock = conn().prepareStatement(updateStockSql)) {
                    psStock.setInt(1, count);
                    psStock.setLong(2, id);
                    psStock.setInt(3, count);
                    psStock.executeUpdate();
                } catch (SQLException ignored) {}
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new DaoException("Could not select/purchase product", e);
        }
    }

    @Override
    public boolean updateQuantities(long id, int quantity, int soldQuantity) {
        ensureColumnsExist();
        String sql = "UPDATE products SET quantity = ?, sold_quantity = ? WHERE product_id = ?";
        String sqlStock = "UPDATE stocks SET quantity = ?, available_quantity = ?, last_updated = CURRENT_TIMESTAMP WHERE product_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, quantity);
            ps.setInt(2, soldQuantity);
            ps.setLong(3, id);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                try (PreparedStatement psStock = conn().prepareStatement(sqlStock)) {
                    psStock.setInt(1, quantity + soldQuantity);
                    psStock.setInt(2, quantity);
                    psStock.setLong(3, id);
                    psStock.executeUpdate();
                } catch (SQLException ignored) {}
            }
            return updated;
        } catch (SQLException e) {
            throw new DaoException("Could not update quantities", e);
        }
    }

    @Override
    public boolean deleteProduct(long id) {
        Connection c = null;
        boolean originalAutoCommit = true;
        try {
            c = conn();
            originalAutoCommit = c.getAutoCommit();
            c.setAutoCommit(false);

            // 1. Delete stock transactions for the product's stock
            String sqlTransactions = "DELETE FROM stock_transactions WHERE stock_id IN (SELECT stock_id FROM stocks WHERE product_id = ?)";
            try (PreparedStatement ps = c.prepareStatement(sqlTransactions)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            // 2. Delete stocks referencing this product
            String sqlStocks = "DELETE FROM stocks WHERE product_id = ?";
            try (PreparedStatement ps = c.prepareStatement(sqlStocks)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            // 3. Delete supplier_products referencing this product
            String sqlSupplierProducts = "DELETE FROM supplier_products WHERE product_id = ?";
            try (PreparedStatement ps = c.prepareStatement(sqlSupplierProducts)) {
                ps.setLong(1, id);
                ps.executeUpdate();
            }

            // 4. Finally delete the product itself
            String sqlProduct = "DELETE FROM products WHERE product_id = ?";
            boolean deleted;
            try (PreparedStatement ps = c.prepareStatement(sqlProduct)) {
                ps.setLong(1, id);
                deleted = ps.executeUpdate() > 0;
            }

            c.commit();
            return deleted;
        } catch (SQLException e) {
            if (c != null) {
                try { c.rollback(); } catch (SQLException ignored) {}
            }
            throw new DaoException("Could not delete product", e);
        } finally {
            if (c != null) {
                try { c.setAutoCommit(originalAutoCommit); } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public Optional<Product> getProductById(long id) {
        ensureColumnsExist();
        String sql = "SELECT * FROM products WHERE product_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Could not read product", e);
        }
    }

    private static boolean autoSeeded = false;

    @Override
    public List<Product> getAllProducts() {
        ensureColumnsExist();
        String sql = "SELECT * FROM products ORDER BY product_id DESC";
        List<Product> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DaoException("Could not list products", e);
        }

        if (!autoSeeded && list.size() <= 1) {
            autoSeeded = true;
            seedSampleProducts();
            return getAllProducts();
        }
        return list;
    }

    @Override
    public List<Product> searchProducts(String keyword) {
        ensureColumnsExist();
        String sql = "SELECT * FROM products WHERE product_name LIKE ? OR description LIKE ? ORDER BY product_id DESC";
        List<Product> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            String q = "%" + keyword + "%";
            ps.setString(1, q);
            ps.setString(2, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Could not search products", e);
        }
    }

    private Product mapRow(ResultSet rs) throws SQLException {
        String img = null;
        try {
            img = rs.getString("image_url");
        } catch (SQLException ignored) {}

        int qty = 0;
        try {
            qty = rs.getInt("quantity");
        } catch (SQLException ignored) {}

        int sold = 0;
        try {
            sold = rs.getInt("sold_quantity");
        } catch (SQLException ignored) {}

        int minStock = 5;
        try {
            int ms = rs.getInt("min_stock");
            if (!rs.wasNull()) minStock = ms;
        } catch (SQLException ignored) {}

        String expiry = null;
        try {
            expiry = rs.getString("expiry_date");
        } catch (SQLException ignored) {}

        return new Product(
            rs.getLong("product_id"),
            rs.getString("product_name"),
            rs.getString("description"),
            rs.getBigDecimal("price"),
            rs.getLong("category_id"),
            qty,
            sold,
            minStock,
            expiry,
            img
        );
    }

    @Override
    public boolean adjustStock(long productId, int amount, String type, String reason) {
        ensureColumnsExist();
        boolean isAdd = "ADD".equalsIgnoreCase(type) || "IN".equalsIgnoreCase(type);
        String sql = isAdd
            ? "UPDATE products SET quantity = quantity + ? WHERE product_id = ?"
            : "UPDATE products SET quantity = GREATEST(0, quantity - ?) WHERE product_id = ?";

        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setInt(1, amount);
            ps.setLong(2, productId);
            boolean updated = ps.executeUpdate() > 0;
            if (updated) {
                String syncStockSql = isAdd 
                    ? "UPDATE stocks SET available_quantity = available_quantity + ?, quantity = quantity + ?, last_updated = CURRENT_TIMESTAMP WHERE product_id = ?"
                    : "UPDATE stocks SET available_quantity = GREATEST(0, available_quantity - ?), quantity = GREATEST(0, quantity - ?), last_updated = CURRENT_TIMESTAMP WHERE product_id = ?";
                try (PreparedStatement psStock = conn().prepareStatement(syncStockSql)) {
                    psStock.setInt(1, amount);
                    psStock.setInt(2, amount);
                    psStock.setLong(3, productId);
                    psStock.executeUpdate();
                } catch (SQLException ignored) {}

                String transSql = "INSERT INTO stock_transactions (stock_id, type, quantity, reference) " +
                    "SELECT stock_id, ?, ?, ? FROM stocks WHERE product_id = ?";
                try (PreparedStatement psTrans = conn().prepareStatement(transSql)) {
                    psTrans.setString(1, type.toUpperCase());
                    psTrans.setInt(2, amount);
                    psTrans.setString(3, reason != null ? reason : ("Manual " + type + " Adjustment"));
                    psTrans.setLong(4, productId);
                    psTrans.executeUpdate();
                } catch (SQLException ignored) {}
            }
            return updated;
        } catch (SQLException e) {
            throw new DaoException("Could not adjust stock", e);
        }
    }

    @Override
    public void seedSampleProducts() {
        ensureColumnsExist();
        List<String> existingNames = new ArrayList<>();
        try (Statement st = conn().createStatement();
             ResultSet rs = st.executeQuery("SELECT product_name FROM products")) {
            while (rs.next()) {
                existingNames.add(rs.getString(1).toLowerCase().trim());
            }
        } catch (SQLException ignored) {}

        Object[][] samples = new Object[][] {
            {"MacBook Pro 14\" M3", "Apple Silicon M3, 16GB RAM, 512GB SSD Space Gray", 1599.00, 1L, 15, 9},
            {"Samsung 4K Ultra HD TV 55\"", "Crystal 4K display with HDR10+ and Smart Tizen OS", 479.99, 1L, 12, 18},
            {"Logitech MX Master Wireless Mouse", "Ergonomic precision mouse with Bluetooth & USB receiver", 69.50, 1L, 40, 22},
            {"Levi's Classic Denim Jacket", "Premium vintage trucker denim jacket 100% cotton", 89.00, 2L, 25, 14},
            {"Nike Air Zoom Pegasus Running Shoes", "Lightweight responsive cushioning for all runners", 119.50, 2L, 18, 30},
            {"Italian Arabica Coffee Beans (1kg)", "Whole bean dark roast authentic espresso blend", 22.00, 3L, 50, 42},
            {"Extra Virgin Olive Oil (1L)", "Single origin cold-pressed Greek extra virgin olive oil", 18.50, 3L, 0, 35},
            {"Ergonomic Mesh Office Chair", "High-back desk chair with 3D lumbar support and headrest", 229.00, 5L, 8, 16},
            {"Stainless Steel Cookware Set (10-pc)", "Tri-ply encapsulated base pots and non-stick pans", 149.99, 4L, 10, 20}
        };

        String sqlProduct = "INSERT INTO products (product_name, description, price, category_id, image_url, quantity, sold_quantity) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlStock = "INSERT INTO stocks (product_id, quantity, available_quantity) VALUES (?, ?, ?)";

        for (Object[] item : samples) {
            String name = (String) item[0];
            if (existingNames.contains(name.toLowerCase().trim())) {
                continue;
            }
            String desc = (String) item[1];
            double price = (Double) item[2];
            long catId = (Long) item[3];
            int qty = (Integer) item[4];
            int sold = (Integer) item[5];

            try (PreparedStatement ps = conn().prepareStatement(sqlProduct, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, name);
                ps.setString(2, desc);
                ps.setBigDecimal(3, java.math.BigDecimal.valueOf(price));
                ps.setLong(4, catId);
                ps.setString(5, null);
                ps.setInt(6, qty);
                ps.setInt(7, sold);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        long newId = keys.getLong(1);
                        try (PreparedStatement psStock = conn().prepareStatement(sqlStock)) {
                            psStock.setLong(1, newId);
                            psStock.setInt(2, qty + sold);
                            psStock.setInt(3, qty);
                            psStock.executeUpdate();
                        } catch (SQLException ignored) {}
                    }
                }
            } catch (SQLException ignored) {}
        }
    }
}