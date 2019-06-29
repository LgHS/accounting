grant usage on schema accounting to lghs_accounting_app;
grant select on accounting.flyway_schema_history to lghs_accounting_app;

create table accounting.accounts (
    id uuid primary key default uuid_generate_v4(),
    name text unique not null,
    description text,
    current_amount numeric(10, 2) not null default 0
);
grant select, insert, update, delete on table accounting.accounts to lghs_accounting_app;
