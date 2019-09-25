create type accounting.subscription_type as enum ('YEARLY', 'MONTHLY');

create table accounting.subscriptions (
    id uuid primary key,
    movement_id uuid not null references accounting.movements(id),
    encoder_id uuid not null references accounting.users(uuid),
    member_id uuid not null references accounting.users(uuid),
    type accounting.subscription_type not null,
    comment text,
    start_date date not null,
    end_date date not null,
    check (start_date < end_date),
    check (date_trunc('month', start_date) = start_date),
    check (date_trunc('month', end_date + '1 day'::interval) = end_date + '1 day'::interval)
);

grant
    select, insert, update, delete
    on table accounting.subscriptions
    to lghs_accounting_app;
