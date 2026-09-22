ALTER TABLE CART
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE TABLE OUTBOX_EVENT (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    topic VARCHAR(255) NOT NULL,
    event_key VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP,
    attempts INTEGER NOT NULL DEFAULT 0,
    last_error TEXT
);

CREATE INDEX idx_outbox_pending
    ON OUTBOX_EVENT (created_at)
    WHERE published_at IS NULL;
