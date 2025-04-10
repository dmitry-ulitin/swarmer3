CREATE TABLE IF NOT EXISTS account_addresses (
    account_id bigint PRIMARY KEY REFERENCES accounts(id) ON DELETE CASCADE,
    chain text NOT NULL,
    address text NOT NULL
);