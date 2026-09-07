CREATE TABLE PRODUCT (
                       id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
                       name VARCHAR(40) NOT NULL,
                       active BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at TIMESTAMP,
                       updated_at TIMESTAMP
);