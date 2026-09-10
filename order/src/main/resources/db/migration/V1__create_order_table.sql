-- V1__create_order_table.sql
-- Creates ORDER and ITEM tables to match Java models and adds helpful indexes.

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Orders table (quoted to match @Table(name = "ORDER") in Java)
CREATE TABLE IF NOT EXISTS "ORDER" (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  customer_id UUID NOT NULL,
  total_price NUMERIC(12,2) NOT NULL DEFAULT 0.00,
  status VARCHAR(50) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Items table (quoted to match @Table(name = "ITEM") in Java)
CREATE TABLE IF NOT EXISTS "ITEM" (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id UUID NOT NULL REFERENCES "ORDER"(id) ON DELETE CASCADE,
  product_id UUID NOT NULL,
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  price NUMERIC(12,2) NOT NULL
);

-- Indexes to speed common queries
CREATE INDEX IF NOT EXISTS idx_order_customer_id ON "ORDER"(customer_id);
CREATE INDEX IF NOT EXISTS idx_order_created_at_desc ON "ORDER"(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_order_status ON "ORDER"(status);

CREATE INDEX IF NOT EXISTS idx_item_order_id ON "ITEM"(order_id);
CREATE INDEX IF NOT EXISTS idx_item_product_id ON "ITEM"(product_id);
CREATE INDEX IF NOT EXISTS idx_item_product_order ON "ITEM"(product_id, order_id);

-- Optional: trigger to keep updated_at current on UPDATE
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_set_updated_at ON "ORDER";
CREATE TRIGGER trg_set_updated_at
BEFORE UPDATE ON "ORDER"
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- NOTE:
-- If the application expects lowercase table names (orders/items) or unquoted identifiers,
-- update the Java @Table annotations or this migration accordingly.
