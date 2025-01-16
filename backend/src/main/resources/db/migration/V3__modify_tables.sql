alter table rules drop column party_id;
ALTER TABLE acl ADD PRIMARY KEY (group_id, user_id);
alter table acl drop column deleted;

