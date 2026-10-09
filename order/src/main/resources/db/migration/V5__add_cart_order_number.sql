CREATE SEQUENCE cart_order_number_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE CART ADD COLUMN order_number BIGINT;

UPDATE CART c
SET order_number = nextval('cart_order_number_seq')
FROM (SELECT id FROM CART ORDER BY created_at, id) o
WHERE c.id = o.id;

ALTER TABLE CART ALTER COLUMN order_number SET DEFAULT nextval('cart_order_number_seq');
ALTER TABLE CART ALTER COLUMN order_number SET NOT NULL;
ALTER TABLE CART ADD CONSTRAINT uk_cart_order_number UNIQUE (order_number);
