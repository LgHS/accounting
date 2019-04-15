# Accounting application for the LgHS

## Setup

Create yourself a nice cosy space from your `psql` prompt:

```sql
create user lghs_accounting_root with password 'lghs_account_root_password'; -- change it really
create database lghs_accounting owner lghs_accounting_root;

\c lghs_accounting
revoke all on schema public from public;

create user lghs_accounting_app;
grant connect on database lghs_accounting to lghs_accounting_app;

create extension "uuid-ossp";
create extension pgcrypto;
```
