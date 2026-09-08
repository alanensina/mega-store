CREATE TABLE STOCK (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       product_id UUID NOT NULL,
                       quantity INTEGER NOT NULL,
                       CONSTRAINT fk_stock_product FOREIGN KEY (product_id) REFERENCES Product (id),
                       CONSTRAINT uq_stock_product_id UNIQUE (product_id)
);