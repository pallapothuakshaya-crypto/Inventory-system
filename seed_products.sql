-- =============================================================
-- INVENTORY MANAGEMENT SYSTEM (IMS) - SAMPLE PRODUCTS SEED
-- Database: inventory_db (MySQL Workbench)
-- =============================================================

USE inventory_db;

-- 1. Ensure Categories exist
INSERT IGNORE INTO categories (category_id, category_name, description) VALUES 
(1, 'Electronics', 'Smartphones, Laptops, Accessories'), 
(2, 'Clothing', 'Apparel, Footwear, Fashion'), 
(3, 'Groceries', 'Food, Beverages, Daily Essentials'), 
(4, 'Home & Kitchen', 'Furniture, Cookware, Appliances'), 
(5, 'Office Supplies', 'Stationery, Desks, Printers');

-- 2. Insert Diverse Sample Products (both in-stock and sold-out items)
INSERT INTO products (product_name, description, price, category_id, quantity, sold_quantity) VALUES 
('MacBook Pro 14" M3', 'Apple Silicon M3, 16GB RAM, 512GB SSD Space Gray', 1599.00, 1, 15, 9),
('Samsung 4K Ultra HD TV 55"', 'Crystal 4K display with HDR10+ and Smart Tizen OS', 479.99, 1, 12, 18),
('Logitech MX Master Wireless Mouse', 'Ergonomic precision mouse with Bluetooth & USB receiver', 69.50, 1, 40, 22),
('Levi\'s Classic Denim Jacket', 'Premium vintage trucker denim jacket 100% cotton', 89.00, 2, 25, 14),
('Nike Air Zoom Pegasus Running Shoes', 'Lightweight responsive cushioning for all runners', 119.50, 2, 18, 30),
('Italian Arabica Coffee Beans (1kg)', 'Whole bean dark roast authentic espresso blend', 22.00, 3, 50, 42),
('Extra Virgin Olive Oil (1L)', 'Single origin cold-pressed Greek extra virgin olive oil', 18.50, 3, 0, 35),
('Ergonomic Mesh Office Chair', 'High-back desk chair with 3D lumbar support and headrest', 229.00, 5, 8, 16),
('Stainless Steel Cookware Set (10-pc)', 'Tri-ply encapsulated base pots and non-stick pans', 149.99, 4, 10, 20);

-- 3. Synchronize Stocks Table
INSERT IGNORE INTO stocks (product_id, quantity, available_quantity)
SELECT product_id, quantity + sold_quantity, quantity 
FROM products 
WHERE product_id NOT IN (SELECT product_id FROM stocks);

-- 4. Verify Total Products
SELECT product_id, product_name, price, quantity, sold_quantity FROM products ORDER BY product_id ASC;
