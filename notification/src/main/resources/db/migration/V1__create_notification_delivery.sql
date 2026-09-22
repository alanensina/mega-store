CREATE TABLE NOTIFICATION_DELIVERY (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    delivered_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_notification_delivery_event UNIQUE (order_id, notification_type)
);
