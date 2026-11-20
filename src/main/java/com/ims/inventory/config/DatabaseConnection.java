package com.ims.inventory.config;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class DatabaseConnection {

    private static DatabaseConnection instance;

    private final String url;
    private final String username;
    private final String password;
    private Connection connection;
    private boolean usingEmbedded = false;
    private boolean schemaInitialized = false;

    private DatabaseConnection() {
        Properties props = new Properties();
        try (InputStream in =
                 DatabaseConnection.class.getResourceAsStream("/application.properties")) {
            if (in == null) {
                throw new IllegalStateException("application.properties not found");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot read application.properties", e);
        }
        this.url      = pick(props, "DB_URL",      "spring.datasource.url");
        this.username = pick(props, "DB_USERNAME", "spring.datasource.username");
        this.password = pick(props, "DB_PASSWORD", "spring.datasource.password");
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public synchronized Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed() || !connection.isValid(2)) {
            if (connection != null) {
                try { connection.close(); } catch (SQLException ignored) { }
            }

            if (!usingEmbedded) {
                try {
                    // Try direct connection to MySQL
                    connection = DriverManager.getConnection(url, username, password);
                    System.out.println("==================================================================");
                    System.out.println(">>> MYSQL WORKBENCH CONNECTED: Successfully connected to MySQL at localhost:3306/inventory_db");
                    System.out.println("==================================================================");
                    initSchema(connection);
                } catch (SQLException ex) {
                    System.err.println(">>> Warning: Direct MySQL connection failed: " + ex.getMessage());
                    // Try connecting to MySQL server root to create database inventory_db
                    boolean recovered = false;
                    try {
                        String rootUrl = "jdbc:mysql://localhost:3306/?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
                        try (Connection rootConn = DriverManager.getConnection(rootUrl, username, password);
                             Statement st = rootConn.createStatement()) {
                            st.execute("CREATE DATABASE IF NOT EXISTS inventory_db");
                            System.out.println(">>> Created MySQL database 'inventory_db' in MySQL Workbench!");
                        }
                        connection = DriverManager.getConnection(url, username, password);
                        initSchema(connection);
                        recovered = true;
                    } catch (SQLException rootEx) {
                        System.err.println(">>> Could not auto-create MySQL database: " + rootEx.getMessage());
                    }

                    if (!recovered) {
                        System.out.println(">>> Note: MySQL not reachable or credentials mismatch. Falling back to embedded H2 database...");
                        usingEmbedded = true;
                    }
                }
            }

            if (usingEmbedded) {
                String fallbackUrl = "jdbc:h2:file:./inventory_data;MODE=MySQL;DATABASE_TO_UPPER=FALSE;CASE_INSENSITIVE_IDENTIFIERS=TRUE";
                connection = DriverManager.getConnection(fallbackUrl, "sa", "");
                initSchema(connection);
            }
        }
        return connection;
    }

    private synchronized void initSchema(Connection c) {
        if (schemaInitialized) return;
        try (Statement stmt = c.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "user_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL UNIQUE, " +
                "password VARCHAR(100) NOT NULL, " +
                "role VARCHAR(20) NOT NULL DEFAULT 'USER', " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)");

            try {
                stmt.execute("UPDATE users SET role = 'USER' WHERE LOWER(username) != 'admin' AND role = 'ADMIN'");
            } catch (SQLException ignored) {}

            try { stmt.execute("ALTER TABLE users ADD COLUMN email VARCHAR(100)"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN phone VARCHAR(20)"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE users ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'"); } catch (SQLException ignored) {}

            stmt.execute("CREATE TABLE IF NOT EXISTS categories (" +
                "category_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "category_name VARCHAR(100) NOT NULL UNIQUE, " +
                "description TEXT, " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS suppliers (" +
                "supplier_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "supplier_name VARCHAR(100) NOT NULL, " +
                "contact_person VARCHAR(100), " +
                "phone VARCHAR(20), " +
                "email VARCHAR(100), " +
                "address TEXT, " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                "product_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "product_name VARCHAR(100) NOT NULL, " +
                "description TEXT, " +
                "price DECIMAL(10,2) NOT NULL, " +
                "category_id BIGINT NOT NULL, " +
                "quantity INT NOT NULL DEFAULT 0, " +
                "sold_quantity INT NOT NULL DEFAULT 0, " +
                "image_url VARCHAR(255), " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)");

            try { stmt.execute("ALTER TABLE products ADD COLUMN quantity INT NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE products ADD COLUMN sold_quantity INT NOT NULL DEFAULT 0"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE products ADD COLUMN image_url VARCHAR(255)"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE products ADD COLUMN min_stock INT NOT NULL DEFAULT 5"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE products ADD COLUMN expiry_date VARCHAR(20)"); } catch (SQLException ignored) {}

            stmt.execute("CREATE TABLE IF NOT EXISTS stocks (" +
                "stock_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "product_id BIGINT NOT NULL UNIQUE, " +
                "quantity INT NOT NULL DEFAULT 0, " +
                "available_quantity INT NOT NULL DEFAULT 0, " +
                "last_updated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS stock_transactions (" +
                "transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "stock_id BIGINT NOT NULL, " +
                "type VARCHAR(10) NOT NULL, " +
                "quantity INT NOT NULL, " +
                "transaction_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                "reference VARCHAR(100))");

            stmt.execute("CREATE TABLE IF NOT EXISTS supplier_products (" +
                "supplier_product_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "supplier_id BIGINT NOT NULL, " +
                "product_id BIGINT NOT NULL, " +
                "supply_price DECIMAL(10,2) NOT NULL, " +
                "lead_time INT NOT NULL)");

            stmt.execute("CREATE TABLE IF NOT EXISTS activity_logs (" +
                "log_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "action_type VARCHAR(50) NOT NULL, " +
                "entity_type VARCHAR(50) NOT NULL, " +
                "entity_name VARCHAR(255), " +
                "details TEXT, " +
                "performed_by VARCHAR(50) NOT NULL, " +
                "timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                "order_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "order_number VARCHAR(50) NOT NULL UNIQUE, " +
                "customer_username VARCHAR(50) NOT NULL, " +
                "product_id BIGINT NOT NULL, " +
                "product_name VARCHAR(100) NOT NULL, " +
                "quantity INT NOT NULL, " +
                "unit_price DECIMAL(10,2) NOT NULL, " +
                "total_amount DECIMAL(10,2) NOT NULL, " +
                "status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED', " +
                "payment_status VARCHAR(20) NOT NULL DEFAULT 'PAID', " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)");

            stmt.execute("CREATE TABLE IF NOT EXISTS purchases (" +
                "purchase_id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "po_number VARCHAR(50) NOT NULL UNIQUE, " +
                "supplier_id BIGINT NOT NULL, " +
                "supplier_name VARCHAR(100) NOT NULL, " +
                "product_id BIGINT NOT NULL, " +
                "product_name VARCHAR(100) NOT NULL, " +
                "quantity INT NOT NULL, " +
                "unit_cost DECIMAL(10,2) NOT NULL, " +
                "total_cost DECIMAL(10,2) NOT NULL, " +
                "status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED', " +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)");

            // Pre-seed default categories if empty
            try {
                stmt.execute("INSERT IGNORE INTO categories (category_id, category_name, description) VALUES " +
                    "(1, 'Electronics', 'Smartphones, Laptops, Accessories'), " +
                    "(2, 'Clothing', 'Apparel, Footwear, Fashion'), " +
                    "(3, 'Groceries', 'Food, Beverages, Daily Essentials'), " +
                    "(4, 'Home & Kitchen', 'Furniture, Cookware, Appliances'), " +
                    "(5, 'Office Supplies', 'Stationery, Desks, Printers')");
            } catch (SQLException e) {
                try {
                    stmt.execute("INSERT INTO categories (category_id, category_name, description) VALUES " +
                        "(1, 'Electronics', 'Smartphones, Laptops, Accessories')");
                } catch (SQLException ignored) {}
            }

            // Pre-seed default products if empty or only 1 item
            try {
                try (java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM products")) {
                    if (rs.next() && rs.getInt(1) <= 1) {
                        stmt.execute("INSERT INTO products (product_name, description, price, category_id, quantity, sold_quantity) VALUES " +
                            "('MacBook Pro 14\" M3', 'Apple Silicon M3, 16GB RAM, 512GB SSD Space Gray', 1599.00, 1, 15, 9), " +
                            "('Samsung 4K Ultra HD TV 55\"', 'Crystal 4K display with HDR10+ and Smart Tizen OS', 479.99, 1, 12, 18), " +
                            "('Logitech MX Master Wireless Mouse', 'Ergonomic precision mouse with Bluetooth & USB receiver', 69.50, 1, 40, 22), " +
                            "('Levi\\'s Classic Denim Jacket', 'Premium vintage trucker denim jacket 100% cotton', 89.00, 2, 25, 14), " +
                            "('Nike Air Zoom Pegasus Running Shoes', 'Lightweight responsive cushioning for all runners', 119.50, 2, 18, 30), " +
                            "('Italian Arabica Coffee Beans (1kg)', 'Whole bean dark roast authentic espresso blend', 22.00, 3, 50, 42), " +
                            "('Extra Virgin Olive Oil (1L)', 'Single origin cold-pressed Greek extra virgin olive oil', 18.50, 3, 0, 35), " +
                            "('Ergonomic Mesh Office Chair', 'High-back desk chair with 3D lumbar support and headrest', 229.00, 5, 8, 16), " +
                            "('Stainless Steel Cookware Set (10-pc)', 'Tri-ply encapsulated base pots and non-stick pans', 149.99, 4, 10, 20)");

                        stmt.execute("INSERT IGNORE INTO stocks (product_id, quantity, available_quantity) " +
                            "SELECT product_id, quantity + sold_quantity, quantity FROM products WHERE product_id NOT IN (SELECT product_id FROM stocks)");
                        System.out.println(">>> Pre-seeded sample catalog into MySQL database!");
                    }
                }
            } catch (SQLException ignored) {}

            // Pre-seed default suppliers if empty
            try {
                stmt.execute("INSERT IGNORE INTO suppliers (supplier_id, supplier_name, contact_person, phone, email, address) VALUES " +
                    "(1, 'Apex Tech Global', 'David Miller', '+1-555-0192', 'sales@apextech.com', '100 Silicon Way, San Jose, CA'), " +
                    "(2, 'Vogue Textiles Ltd', 'Sarah Jenkins', '+1-555-0381', 'orders@voguetextiles.com', '45 Fashion Blvd, New York, NY'), " +
                    "(3, 'Heritage Roasters', 'Marco Rossi', '+1-555-0723', 'marco@heritageroast.com', '12 Coffee Port, Seattle, WA'), " +
                    "(4, 'ErgoCraft Furnishings', 'Lisa Chen', '+1-555-0844', 'lisa@ergocraft.com', '88 Industrial Park, Chicago, IL')");
            } catch (SQLException ignored) {}

            // Pre-seed default orders if empty
            try {
                stmt.execute("INSERT IGNORE INTO orders (order_id, order_number, customer_username, product_id, product_name, quantity, unit_price, total_amount, status, payment_status) VALUES " +
                    "(1, 'ORD-1001', 'user', 2, 'Wireless Headphones', 2, 199.00, 398.00, 'COMPLETED', 'PAID'), " +
                    "(2, 'ORD-1002', 'lekha', 1, 'MacBook Pro 14\" M3', 1, 1599.00, 1599.00, 'COMPLETED', 'PAID'), " +
                    "(3, 'ORD-1003', 'user', 4, 'Levi\\'s Classic Denim Jacket', 1, 89.00, 89.00, 'COMPLETED', 'PAID')");
            } catch (SQLException ignored) {}

            // Pre-seed default purchases if empty
            try {
                stmt.execute("INSERT IGNORE INTO purchases (purchase_id, po_number, supplier_id, supplier_name, product_id, product_name, quantity, unit_cost, total_cost, status) VALUES " +
                    "(1, 'PO-2001', 1, 'Apex Tech Global', 1, 'MacBook Pro 14\" M3', 10, 1200.00, 12000.00, 'RECEIVED'), " +
                    "(2, 'PO-2002', 1, 'Apex Tech Global', 2, 'Wireless Headphones', 25, 120.00, 3000.00, 'RECEIVED'), " +
                    "(3, 'PO-2003', 2, 'Vogue Textiles Ltd', 4, 'Levi\\'s Classic Denim Jacket', 20, 50.00, 1000.00, 'RECEIVED')");
            } catch (SQLException ignored) {}

            schemaInitialized = true;
            System.out.println(">>> Database schema verified and initialized! Ready for MySQL Workbench.");
        } catch (SQLException e) {
            System.err.println("Warning during schema setup: " + e.getMessage());
        }
    }

    private static String pick(Properties p, String envName, String propKey) {
        String fromEnv = System.getenv(envName);
        return (fromEnv != null && !fromEnv.isBlank()) ? fromEnv : p.getProperty(propKey);
    }
}