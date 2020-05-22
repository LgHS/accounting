create table accounting.movement_tags (
    movement_id uuid references accounting.movements(id),
    content varchar(30) not null check ( content = trim(both ' ' from content) ),

    primary key (movement_id, content)
);

grant select, insert, update, delete on table accounting.movement_tags to lghs_accounting_app;
