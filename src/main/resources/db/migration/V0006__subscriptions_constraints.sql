create unique index subscriptions_movement_id_unique
    on accounting.subscriptions(movement_id);

alter table accounting.subscriptions
    add exclude using gist (member_id with =, type with =, daterange(start_date, end_date, '[]') with &&)
;
