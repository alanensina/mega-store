CREATE TABLE STOCK (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       product_id UUID NOT NULL,
                       quantity INTEGER NOT NULL,
                       CONSTRAINT fk_stock_product FOREIGN KEY (product_id) REFERENCES PRODUCT (id),
                       CONSTRAINT uq_stock_product_id UNIQUE (product_id)
);

-- Indexes to improve common queries on stock
-- Index on quantity to speed up range queries and ORDER BY quantity
CREATE INDEX IF NOT EXISTS idx_stock_quantity ON STOCK (quantity);

-- Partial index for out-of-stock or low-stock queries (e.g., WHERE quantity <= 0)
CREATE INDEX IF NOT EXISTS idx_stock_out_of_stock ON STOCK (product_id) WHERE quantity <= 0;