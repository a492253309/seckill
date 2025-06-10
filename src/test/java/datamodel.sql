CREATE TABLE inventory (
                           sku VARCHAR(50) PRIMARY KEY,
                           stock INT NOT NULL,
                           version INT NOT NULL DEFAULT 0
);
CREATE TABLE orders (
                        order_id VARCHAR(50) PRIMARY KEY,
                        user_id VARCHAR(50),
                        sku VARCHAR(50),
                        quantity INT,
                        status VARCHAR(20),
                        create_time TIMESTAMP
);
CREATE TABLE dead_letter_log (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 order_id VARCHAR(50),
                                 message TEXT,
                                 create_time TIMESTAMP,
                                 INDEX idx_order_id (order_id)
);
