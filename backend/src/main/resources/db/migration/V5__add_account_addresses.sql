-- Add chain and address fields to the accounts table
ALTER TABLE accounts ADD COLUMN chain text;
ALTER TABLE accounts ADD COLUMN address text;
ALTER TABLE accounts ADD COLUMN scale integer not null default 2;

-- Update the transactions table to use the new scale
update transactions set debit = debit * 100;
update transactions set credit = credit * 100;
alter table transactions alter column debit type numeric(24, 0);
alter table transactions alter column credit type numeric(24, 0);

-- Update the accounts table to use the new scale
update accounts set start_balance = start_balance * 100;
alter table accounts alter column start_balance type numeric(24, 0);
