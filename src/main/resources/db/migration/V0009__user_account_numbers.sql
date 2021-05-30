create table accounting.user_account_numbers (
    user_id uuid not null references users(uuid),
    account_number text unique not null,
    validated boolean not null default false,
    encoding_date timestamp not null default now(),
    primary key (user_id, account_number)
);

grant select, insert, update, delete on accounting.user_account_numbers to lghs_accounting_app;