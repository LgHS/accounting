ALTER TABLE accounting.users DROP CONSTRAINT users_pkey;
ALTER TABLE accounting.users ADD PRIMARY KEY (uuid);
ALTER TABLE accounting.users DROP COLUMN id;
