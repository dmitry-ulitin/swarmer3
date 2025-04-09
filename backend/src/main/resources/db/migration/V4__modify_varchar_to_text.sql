alter table users alter column email type text;
alter table users alter column name type text;
alter table users alter column password type text;
alter table users alter column currency type text;

alter table categories alter column name type text;

alter table account_groups alter column name type text;

alter table accounts alter column name type text;
alter table accounts alter column currency type text;

alter table acl alter column name type text;

alter table transactions alter column currency type text;
alter table transactions alter column party type text;
alter table transactions alter column details type text;

alter table rules alter column condition_value type text;
