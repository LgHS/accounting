create type accounting.user_role as enum ('ROLE_MEMBER', 'ROLE_ADMIN', 'ROLE_TREASURER');

create table accounting.users (
    id integer primary key,
    uuid uuid unique not null,
    name text not null,
    username text unique not null,
    email text unique not null,
    roles accounting.user_role[] not null default array[ 'ROLE_MEMBER'::accounting.user_role ]
);

grant select, insert, update, delete on table accounting.users to lghs_accounting_app;
