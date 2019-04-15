grant usage on schema accounting to lghs_accounting_app ;


create table accounts (
    id uuid primary key,
);

grant select, insert, update, delete on table accounts to lghs_accounting_app
