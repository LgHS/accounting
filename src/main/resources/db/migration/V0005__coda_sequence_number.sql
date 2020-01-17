alter table accounting.codas
    add column sequence_number int not null default -1; -- filled in manually

alter table accounting.codas
    alter column sequence_number drop default;