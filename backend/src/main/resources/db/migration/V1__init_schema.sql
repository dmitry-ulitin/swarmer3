CREATE TABLE IF NOT EXISTS users (
	id bigserial primary key, 
	email varchar(255) not null, 
	name varchar(255) not null, 
	password varchar(255) not null,
    enabled boolean not null default true,
	currency varchar(5) not null default 'EUR', 
	created timestamp not null default now(), 
	updated timestamp not null default now()
);

CREATE TABLE IF NOT EXISTS categories (
	id bigserial primary key, 
	owner_id integer references users (id), 
	parent_id integer references categories (id), 
	name varchar(255) not null,
	created timestamp not null default now(), 
	updated timestamp not null default now()
);

INSERT INTO categories (id, name) VALUES(1,'Expense');
INSERT INTO categories (id, name) VALUES(2, 'Income');
INSERT INTO categories (id, name) VALUES(3, 'Correction');
ALTER SEQUENCE categories_id_seq RESTART WITH 4;

CREATE TABLE IF NOT EXISTS account_groups (
	id bigserial primary key, 
	owner_id integer not null references users (id), 
	name varchar(255) not null, 
	deleted boolean not null default false, 
	created timestamp not null default now(), 
	updated timestamp not null default now()
);

CREATE TABLE IF NOT EXISTS accounts (
	id bigserial primary key,
	group_id integer not null references account_groups (id), 
	name varchar(255), 
	currency varchar(5) not null, 
	start_balance numeric(15, 2) default 0, 
	deleted boolean not null default false, 
	created timestamp not null default now(), 
	updated timestamp not null default now()
);

CREATE TABLE IF NOT EXISTS acl (
	group_id integer not null references account_groups (id), 
	user_id integer not null references users (id), 
	is_readonly boolean not null, 
	is_admin boolean not null, 
	name varchar(255), 
	deleted boolean, 
	created timestamp not null default now(), 
	updated timestamp not null default now()
);

CREATE TABLE IF NOT EXISTS transactions (
	id bigserial primary key,
	owner_id integer not null references users (id), 
	opdate timestamp not null, 
	account_id integer references accounts (id), 
	debit numeric(12, 2) not null, 
	recipient_id integer references accounts (id), 
	credit numeric(12, 2) not null, 
	category_id integer references categories (id), 
	currency varchar(5), 
	party varchar(1024), 
	details varchar(1024), 
	created timestamp not null default now(), 
	updated timestamp not null default now()
);

CREATE TABLE IF NOT EXISTS rules (
	id bigserial primary key, 
	owner_id integer not null references users (id), 
	transaction_type integer not null,
	condition_type integer not null, 
	condition_value varchar(1024) not null, 
	category_id integer references categories (id), 
	party_id integer references accounts (id),
	created timestamp not null default now(), 
	updated timestamp not null default now()
);

