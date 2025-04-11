-- Add chain and address fields to the accounts table
ALTER TABLE accounts ADD COLUMN chain text;
ALTER TABLE accounts ADD COLUMN address text;
