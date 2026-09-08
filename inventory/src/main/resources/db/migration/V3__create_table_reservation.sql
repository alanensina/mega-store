CREATE TABLE RESERVATION (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id uuid UNIQUE,
    status VARCHAR(50),
    reserved_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE RESERVATION_ITEM (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    reservation_id uuid NOT NULL,
    product_id uuid NOT NULL,
    quantity integer NOT NULL,
    CONSTRAINT fk_reservation_item_reservation FOREIGN KEY (reservation_id) REFERENCES RESERVATION (id),
    CONSTRAINT fk_reservation_item_product FOREIGN KEY (product_id) REFERENCES product (id)
);

CREATE INDEX IF NOT EXISTS idx_reservation_item_reservation ON RESERVATION_ITEM (reservation_id);
CREATE INDEX IF NOT EXISTS idx_reservation_item_product ON RESERVATION_ITEM (product_id);
CREATE INDEX IF NOT EXISTS idx_reservation_status ON RESERVATION (status);
CREATE INDEX IF NOT EXISTS idx_reservation_reserved_at ON RESERVATION (reserved_at);
