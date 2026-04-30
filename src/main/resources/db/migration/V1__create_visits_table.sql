CREATE TABLE visits (
    visit_id             VARCHAR(36)   NOT NULL,
    customer_id          VARCHAR(255)  NOT NULL,
    vehicle_id           VARCHAR(255)  NOT NULL,
    dealer_id            VARCHAR(255)  NOT NULL,
    proposed_time        TIMESTAMPTZ   NOT NULL,
    state                VARCHAR(20)   NOT NULL,
    hold_id              VARCHAR(255),
    confirmation_deadline TIMESTAMPTZ,
    cancel_reason        VARCHAR(1000),
    correlation_id       VARCHAR(255),
    idempotency_key      VARCHAR(255)  NOT NULL,
    created_at           TIMESTAMPTZ   NOT NULL,
    updated_at           TIMESTAMPTZ   NOT NULL,
    PRIMARY KEY (visit_id)
);

CREATE UNIQUE INDEX idx_visits_idempotency_key ON visits (idempotency_key);
CREATE INDEX idx_visits_customer_id ON visits (customer_id);
CREATE INDEX idx_visits_dealer_id   ON visits (dealer_id);
CREATE INDEX idx_visits_state       ON visits (state);
