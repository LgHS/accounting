create table accounting.codas (
    id uuid primary key default uuid_generate_v4(),
    filename text unique not null,
    content bytea not null,
    account_id uuid references accounting.accounts(id) not null
);
grant select, insert, update, delete on table accounting.codas to lghs_accounting_app;

create table accounting.movement_categories (
    id uuid primary key default uuid_generate_v4(),
    name varchar(255) not null,
    description text,
    type varchar (255) not null -- should be type enum (DEBIT, CREDIT)
);
grant select, insert, update, delete on table accounting.movement_categories to lghs_accounting_app;

insert into accounting.movement_categories (name, type) values
    ('Marchandises et services', 'DEBIT'),
    ('Rémunérations', 'DEBIT'),
    ('Services et biens divers', 'DEBIT'),
    ('Autres dépenses', 'DEBIT'),
    ('Cotisations', 'CREDIT'),
    ('Dons et legs', 'CREDIT'),
    ('Subsides', 'CREDIT'),
    ('Autres recettes', 'CREDIT'),
    ('Crédit interne', 'CREDIT'),
    ('Débit interne', 'DEBIT')
;

create table accounting.movements (
    id uuid primary key default uuid_generate_v4(),
    amount numeric(10, 2) not null,
    entry_date date not null,
    account_id uuid not null references accounting.accounts(id),
    coda_id uuid references accounting.codas(id), -- nullable when the coda is missing
    coda_sequence_number int4,
    counter_party_account_number varchar(255),
    counter_party_name varchar(255),
    communication text,
    category_id uuid references accounting.movement_categories(id)
    -- check if coda_id is not null, coda_sequence_number shouldn't be
);
grant select, insert, update, delete on table accounting.movements to lghs_accounting_app;
