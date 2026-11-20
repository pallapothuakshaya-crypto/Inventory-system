USE inventory_db;

-- Tables that don't depend on anything come first.
CREATE TABLE IF NOT EXISTS users (
    user_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    password   VARCHAR(100) NOT NULL,
    role       VARCHAR(20)  NOT NULL DEFAULT 'USER',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS categories (
    category_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    description   TEXT,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS suppliers (
    supplier_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_name  VARCHAR(100) NOT NULL,
    contact_person VARCHAR(100),
    phone          VARCHAR(20),
    email          VARCHAR(100),
    address        TEXT,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tables that point to the ones above come after.
CREATE TABLE IF NOT EXISTS products (
    product_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100)  NOT NULL,
    description  TEXT,
    price        DECIMAL(10,2) NOT NULL CHECK (price > 0),
    category_id  BIGINT        NOT NULL,
    image_url    VARCHAR(255),
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(category_id)
);

CREATE TABLE IF NOT EXISTS stocks (
    stock_id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id         BIGINT NOT NULL UNIQUE,
    quantity           INT    NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    available_quantity INT    NOT NULL DEFAULT 0 CHECK (available_quantity >= 0),
    last_updated       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                       ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

CREATE TABLE IF NOT EXISTS stock_transactions (
    transaction_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_id         BIGINT      NOT NULL,
    type             VARCHAR(10) NOT NULL,
    quantity         INT         NOT NULL CHECK (quantity > 0),
    transaction_date DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reference        VARCHAR(100),
    FOREIGN KEY (stock_id) REFERENCES stocks(stock_id)
);

CREATE TABLE IF NOT EXISTS supplier_products (
    supplier_product_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    supplier_id         BIGINT        NOT NULL,
    product_id          BIGINT        NOT NULL,
    supply_price        DECIMAL(10,2) NOT NULL CHECK (supply_price > 0),
    lead_time           INT           NOT NULL,
    UNIQUE (supplier_id, product_id),
    FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id),
    FOREIGN KEY (product_id)  REFERENCES products(product_id)
);

CREATE TABLE IF NOT EXISTS activity_logs (
    log_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    action_type   VARCHAR(50)  NOT NULL,
    entity_type   VARCHAR(50)  NOT NULL,
    entity_name   VARCHAR(255),
    details       TEXT,
    performed_by  VARCHAR(50)  NOT NULL,
    timestamp     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);