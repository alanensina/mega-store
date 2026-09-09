CREATE TABLE CUSTOMER (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(50) NOT NULL,
    email VARCHAR(50) NOT NULL,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
    );

CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_email
    ON CUSTOMER ((LOWER(email)));

CREATE UNIQUE INDEX IF NOT EXISTS uk_customer_username
    ON CUSTOMER ((LOWER(username)));

CREATE INDEX IF NOT EXISTS idx_customer_name
    ON CUSTOMER ((LOWER(name)));

CREATE INDEX IF NOT EXISTS idx_customer_created_at
    ON CUSTOMER (created_at);

CREATE INDEX IF NOT EXISTS idx_customer_updated_at
    ON CUSTOMER (updated_at);