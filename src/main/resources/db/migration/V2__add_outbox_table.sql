-- Table to store outbox events atomically in the same transaction as transfers
CREATE TABLE outbox_events (
                               id UUID PRIMARY KEY,
                               aggregate_type VARCHAR(50) NOT NULL,
                               aggregate_id UUID NOT NULL,
                               event_type VARCHAR(100) NOT NULL,
                               payload TEXT NOT NULL,
                               status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PROCESSED', 'FAILED')),
                               retry_count INT NOT NULL DEFAULT 0,
                               created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                               processed_at TIMESTAMP WITHOUT TIME ZONE
);

-- Index for the background relay poller to quickly find pending events
CREATE INDEX idx_outbox_status_created ON outbox_events (status, created_at);