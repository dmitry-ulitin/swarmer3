-- Add chain and address fields to the accounts table
ALTER TABLE accounts ADD COLUMN chain text;
ALTER TABLE accounts ADD COLUMN address text;
ALTER TABLE accounts ADD COLUMN scale integer not null default 2;

alter table transactions alter column debit type numeric(20, 8);
alter table transactions alter column credit type numeric(20, 8);