create user lghs_accounting_root;
create database lghs_accounting owner lghs_accounting_root;

\c lghs_accounting
revoke all on schema public from public;

create user lghs_accounting_app;
grant connect on database lghs_accounting to lghs_accounting_app;

grant usage on schema public to lghs_accounting_root;
grant usage on schema public to lghs_accounting_app;

create extension "uuid-ossp";
create extension pgcrypto;
create extension btree_gist;
