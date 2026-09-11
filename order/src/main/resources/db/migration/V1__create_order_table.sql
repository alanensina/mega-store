CREATE TABLE CART (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id UUID NOT NULL,
  total_price NUMERIC(12,2) NOT NULL,
  status VARCHAR(25) NOT NULL,
  created_at TIMESTAMP,
  updated_at TIMESTAMP
);

CREATE TABLE ITEM (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  cart_id UUID NOT NULL REFERENCES CART(id),
  product_id UUID NOT NULL,
  quantity INTEGER,
  price NUMERIC(12,2) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cart_customer_id ON CART(customer_id);
CREATE INDEX IF NOT EXISTS idx_cart_created_at_desc ON CART(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_cart_status ON CART(status);

CREATE INDEX IF NOT EXISTS idx_item_cart_id ON ITEM(cart_id);
CREATE INDEX IF NOT EXISTS idx_item_product_id ON ITEM(product_id);
CREATE INDEX IF NOT EXISTS idx_item_product_cart ON ITEM(product_id, cart_id);
