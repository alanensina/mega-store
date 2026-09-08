CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE PRODUCT (
                       id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
                       name VARCHAR(40) NOT NULL UNIQUE,
                       active BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP,
                       updated_at TIMESTAMP
);

-- Indexes to improve common queries
-- Case-insensitive index for equality searches on name
CREATE INDEX IF NOT EXISTS idx_product_name_lower ON PRODUCT (LOWER(name));

-- Trigram GIN index for fast ILIKE / pattern searches (e.g., WHERE name ILIKE '%foo%')
CREATE INDEX IF NOT EXISTS idx_product_name_trgm ON PRODUCT USING gin (name gin_trgm_ops);

-- Index for boolean filtering on active
CREATE INDEX IF NOT EXISTS idx_product_active ON PRODUCT (active);

-- Composite index for queries that filter by active and search/sort by name
CREATE INDEX IF NOT EXISTS idx_product_active_name_lower ON PRODUCT (active, LOWER(name));

-- Index to optimize queries that order by or filter by creation time
CREATE INDEX IF NOT EXISTS idx_product_created_at ON PRODUCT (created_at);

-- Product names must be unique irrespective of case, uncomment below
CREATE UNIQUE INDEX IF NOT EXISTS uq_product_name_lower ON PRODUCT (LOWER(name));
