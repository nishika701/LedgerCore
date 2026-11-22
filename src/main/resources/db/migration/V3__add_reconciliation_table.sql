-- Add initial_balance to accounts for opening balance baseline tracking
ALTER TABLE accounts ADD COLUMN initial_balance NUMERIC(19, 2) NOT NULL DEFAULT 0.00;

-- Table to store automated reconciliation audit records
CREATE TABLE reconciliation_records (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL,
    recorded_balance NUMERIC(19, 2) NOT NULL,
    calculated_balance NUMERIC(19, 2) NOT NULL,
    discrepancy_amount NUMERIC(19, 2) NOT NULL,
    status VARCHAR(30) NOT NULL CHECK (status IN ('MATCHED', 'DISCREPANCY_DETECTED')),
    reconciled_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    details VARCHAR(500),
    CONSTRAINT fk_reconciliation_account FOREIGN KEY (account_id) REFERENCES accounts (id)
);

-- Index for querying audit history by account and date
CREATE INDEX idx_reconciliation_account_date ON reconciliation_records (account_id, reconciled_at);
