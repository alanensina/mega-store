CREATE TABLE CART_IDEMPOTENCY (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_key VARCHAR(200) NOT NULL,
    username VARCHAR(120) NOT NULL,
    cart_id UUID REFERENCES CART(id),
    created_at TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX uk_cart_idempotency_request_key_username
    ON CART_IDEMPOTENCY (request_key, username);

CREATE INDEX idx_cart_idempotency_cart_id
    ON CART_IDEMPOTENCY (cart_id);
