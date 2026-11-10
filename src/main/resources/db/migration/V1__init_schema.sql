-- 1. Users table
CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       role VARCHAR(50) NOT NULL CHECK (role IN ('USER', 'ADMIN'))
);

-- 2. Accounts table
CREATE TABLE accounts (
                          id UUID PRIMARY KEY,
                          account_number VARCHAR(255) NOT NULL UNIQUE,
                          owner_name VARCHAR(255) NOT NULL,
                          currency VARCHAR(10) NOT NULL,
                          balance NUMERIC(19, 2) NOT NULL CHECK (balance >= 0),
                          user_id UUID NOT NULL,
                          version BIGINT NOT NULL DEFAULT 0,
                          created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                          updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                          CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- 3. Transfers table
CREATE TABLE transfer (
                          id UUID PRIMARY KEY,
                          from_account_id UUID NOT NULL,
                          to_account_id UUID NOT NULL,
                          amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
                          status VARCHAR(50) NOT NULL CHECK (status IN ('PENDING', 'COMPLETED', 'FAILED')),
                          idempotency_key VARCHAR(255) NOT NULL UNIQUE,
                          version BIGINT NOT NULL DEFAULT 0,
                          created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                          CONSTRAINT fk_transfer_from_account FOREIGN KEY (from_account_id) REFERENCES accounts (id),
                          CONSTRAINT fk_transfer_to_account FOREIGN KEY (to_account_id) REFERENCES accounts (id)
);

-- 4. Ledger Entries table (Append-only immutable double-entry ledger)
CREATE TABLE ledger_entry (
                              id UUID PRIMARY KEY,
                              account_id UUID NOT NULL,
                              transfer_id UUID NOT NULL,
                              entry_type VARCHAR(10) NOT NULL CHECK (entry_type IN ('DEBIT', 'CREDIT')),
                              amount NUMERIC(19, 2) NOT NULL CHECK (amount > 0),
                              description VARCHAR(255),
                              created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
                              CONSTRAINT fk_ledger_account FOREIGN KEY (account_id) REFERENCES accounts (id),
                              CONSTRAINT fk_ledger_transfer FOREIGN KEY (transfer_id) REFERENCES transfer (id)
);

-- 5. Notifications table
CREATE TABLE notifications (
                               notification_id UUID PRIMARY KEY,
                               account_id UUID NOT NULL,
                               description VARCHAR(255),
                               notification_type VARCHAR(50) NOT NULL,
                               created_on TIMESTAMP WITHOUT TIME ZONE,
                               CONSTRAINT fk_notifications_account FOREIGN KEY (account_id) REFERENCES accounts (id)
);

-- Indices for high-frequency queries
CREATE INDEX idx_accounts_user_id ON accounts (user_id);
CREATE INDEX idx_ledger_entry_account_id ON ledger_entry (account_id);
CREATE INDEX idx_ledger_entry_transfer_id ON ledger_entry (transfer_id);
CREATE INDEX idx_transfer_from_account ON transfer (from_account_id);
CREATE INDEX idx_transfer_to_account ON transfer (to_account_id);