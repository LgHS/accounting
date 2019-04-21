grant usage on schema accounting to lghs_accounting_app;
grant select on accounting.flyway_schema_history to lghs_accounting_app;

create table accounting.accounts (
    id uuid primary key default uuid_generate_v4(),
    name text unique not null,
    description text
);

grant select, insert, update, delete on table accounting.accounts to lghs_accounting_app
