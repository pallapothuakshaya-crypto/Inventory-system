# Inventory Management System (IMS)

A full-stack, enterprise-ready Inventory & Store Management System built with **Spring Boot 4**, **Java 17**, **JDBC**, **MySQL**, and **Thymeleaf with Bootstrap 5**.

---

## 📌 Project Overview

The Inventory Management System (IMS) provides dual perspectives:
1. **Admin Portal**: Complete backend management for monitoring analytics, low-stock alerts, tracking quantities, sold counts, managing categories, suppliers, and viewing system activity audit logs.
2. **Customer / User Portal**: A customer-facing product catalogue where users can view available stock, place product orders, and receive interactive **"Sold Out / Restock Notification"** popups for completed inventory items.

---

## 📂 Project Directory Structure

```text
inventory/
│
├── .mvn/                                      # Maven wrapper binaries and configuration
├── mvnw                                       # Maven wrapper for Linux/macOS
├── mvnw.cmd                                   # Maven wrapper for Windows
├── run.bat                                    # 1-Click Windows execution script
├── pom.xml                                    # Maven project dependencies & build configuration
├── README.md                                  # Project documentation & structure
│
└── src/
    └── main/
        ├── java/
        │   └── com/ims/inventory/
        │       │
        │       ├── InventoryApplication.java  # Main Spring Boot entry point
        │       │
        │       ├── config/
        │       │   └── DatabaseConnection.java# Singleton JDBC database manager (MySQL & embedded fallback)
        │       │
        │       ├── controller/                # REST API & Web Page routing controllers
        │       │   ├── AuthController.java    # User login, registration, and session management
        │       │   ├── ProductController.java # Product CRUD, customer select order, and restock notify endpoints
        │       │   ├── CategoryController.java# Category CRUD operations
        │       │   ├── SupplierController.java# Supplier CRUD operations
        │       │   ├── HistoryController.java # Activity audit logs retrieval
        │       │   ├── ViewController.java    # Thymeleaf web page template routes
        │       │   └── GlobalExceptionHandler.java # Global REST exception & error response handler
        │       │
        │       ├── model/                     # Data Models (POJOs)
        │       │   ├── User.java              # User account entity (ID, username, password, role)
        │       │   ├── Product.java           # Product entity (ID, name, description, price, category, quantity, soldQuantity)
        │       │   ├── Category.java          # Product category entity
        │       │   ├── Supplier.java          # Supplier entity (contact, phone, email, address)
        │       │   └── HistoryLog.java        # Activity audit log entity
        │       │
        │       ├── dao/                       # Data Access Objects (Raw SQL / JDBC Repositories)
        │       │   ├── UserDAO.java           # User DAO interface
        │       │   ├── UserDAOImpl.java       # User SQL queries & duplicate username handling
        │       │   ├── ProductDAO.java        # Product DAO interface (includes stock decrement & sold count update)
        │       │   ├── ProductDAOImpl.java    # Product SQL queries & stock sync
        │       │   ├── CategoryDAO.java       # Category DAO interface
        │       │   ├── CategoryDAOImpl.java   # Category SQL queries
        │       │   ├── SupplierDAO.java       # Supplier DAO interface
        │       │   ├── SupplierDAOImpl.java   # Supplier SQL queries
        │       │   ├── HistoryDAO.java        # Audit log DAO interface
        │       │   ├── HistoryDAOImpl.java    # Audit log insert & fetch queries
        │       │   ├── DaoException.java      # Base DAO exception
        │       │   └── DuplicateEntryException.java # Unique constraint exception
        │       │
        │       └── service/                   # Business Logic Layer
        │           ├── UserService.java       # User authentication, BCrypt hashing, and pre-seeded defaults
        │           ├── ProductService.java    # Product validation, stock management, and activity logging
        │           ├── CategoryService.java   # Category business logic
        │           ├── SupplierService.java   # Supplier business logic
        │           ├── HistoryService.java    # Activity logging service
        │           ├── BusinessRuleException.java  # Validation error exception
        │           └── ResourceNotFoundException.java # 404 Entity not found exception
        │
        └── resources/
            ├── application.properties         # Server port, database URL, credentials
            │
            └── templates/                     # Frontend Thymeleaf HTML Views
                ├── login.html                 # Sign-in & sign-up page with role picker & quick-demo fill
                ├── dashboard.html             # Admin analytics dashboard with charts & low-stock alerts
                ├── products.html              # Core product catalogue (Admin stock tracking & Customer sold out popup)
                ├── categories.html            # Category management page
                ├── suppliers.html             # Supplier directory and management page
                └── history.html               # System activity audit logs & order history
```

---

## 🗄️ Database Schema (MySQL)

Database Name: `inventory_db`

### 1. `users`
| Column | Type | Description |
|---|---|---|
| `user_id` | BIGINT AUTO_INCREMENT PK | Unique User ID |
| `username` | VARCHAR(50) UNIQUE | Account username |
| `password` | VARCHAR(100) | BCrypt hashed password |
| `role` | VARCHAR(20) | Role: `ADMIN` or `USER` |
| `created_at` | DATETIME | Account creation timestamp |

### 2. `products`
| Column | Type | Description |
|---|---|---|
| `product_id` | BIGINT AUTO_INCREMENT PK | Unique Product ID |
| `product_name` | VARCHAR(100) | Item name |
| `description` | TEXT | Item specifications |
| `price` | DECIMAL(10,2) | Unit selling price |
| `category_id` | BIGINT | Category foreign key |
| `quantity` | INT DEFAULT 0 | Available stock on hand |
| `sold_quantity` | INT DEFAULT 0 | Total units sold |
| `image_url` | VARCHAR(255) | Product image link |
| `created_at` | DATETIME | Timestamp |

### 3. `stocks`
| Column | Type | Description |
|---|---|---|
| `stock_id` | BIGINT AUTO_INCREMENT PK | Stock tracking ID |
| `product_id` | BIGINT UNIQUE | Product reference |
| `quantity` | INT DEFAULT 0 | Total inventory |
| `available_quantity` | INT DEFAULT 0 | Available inventory |
| `last_updated` | DATETIME | Last stock update |

### 4. `categories`
| Column | Type | Description |
|---|---|---|
| `category_id` | BIGINT AUTO_INCREMENT PK | Category ID |
| `category_name` | VARCHAR(100) UNIQUE | Category name |
| `description` | TEXT | Category description |

### 5. `suppliers`
| Column | Type | Description |
|---|---|---|
| `supplier_id` | BIGINT AUTO_INCREMENT PK | Supplier ID |
| `supplier_name` | VARCHAR(100) | Company name |
| `contact_person` | VARCHAR(100) | Representative name |
| `phone` | VARCHAR(20) | Phone number |
| `email` | VARCHAR(100) | Contact email |
| `address` | TEXT | Physical address |

### 6. `activity_logs`
| Column | Type | Description |
|---|---|---|
| `log_id` | BIGINT AUTO_INCREMENT PK | Audit log ID |
| `action_type` | VARCHAR(50) | `CREATE`, `UPDATE`, `DELETE`, `ORDER`, `NOTIFY` |
| `entity_type` | VARCHAR(50) | `PRODUCT`, `USER`, `CATEGORY`, `SUPPLIER` |
| `entity_name` | VARCHAR(255) | Name of entity affected |
| `details` | TEXT | Description of action |
| `performed_by` | VARCHAR(50) | Username who initiated action |
| `timestamp` | DATETIME | Timestamp of event |

---

## 🚀 Key Features

### 🛒 Customer / User Experience (`USER` Role)
- **Direct Store Access**: Automatically lands on `/products` in customer view.
- **Admin Elements Hidden**: All admin controls (`Dashboard`, `Categories`, `Suppliers`, `Add Product`, `Edit`, `Delete`) are completely hidden.
- **In-Stock Orders**: Clicking **"Select Product"** decrements quantity by 1, increments sold by 1, and updates MySQL in real time.
- **Sold Out Popup Modal**: If an item has `0 in stock`, selecting it triggers an interactive popup modal stating:
  > *"Product Sold Out — We will notify you immediately once fresh stock is added!"*
  > Allows the customer to enter contact details to subscribe for restock notifications.

### 🛡️ Admin Experience (`ADMIN` Role)
- **Full Inventory Control**: View exact stock quantities and sold units.
- **Management Tools**: Add new products with initial stock, edit existing inventory numbers, and delete items.
- **Business Dashboard**: View charts, metrics, low-stock warnings, categories, and suppliers.
- **Full Audit History**: Track every transaction, registration, and restock notification request.

---

## ⚙️ Configuration & Setup

### Database Connection (`src/main/resources/application.properties`)
```properties
spring.application.name=inventory

spring.datasource.url=jdbc:mysql://localhost:3306/inventory_db?createDatabaseIfNotExist=true&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD

server.port=8081
```

### Running the Application
In Windows Command Prompt / File Explorer:
```cmd
cd C:\Users\lahar\Downloads\Projects\Projects\inventory
run.bat
```

### Default Credentials
- **Admin**: `admin` / `admin123`
- **Customer**: `user` / `user123`
- **Application URL**: `http://localhost:8081/login`
