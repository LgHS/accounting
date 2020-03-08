create unique index subscriptions_movement_id_unique
    on accounting.subscriptions(movement_id);
create unique index subscriptions_member_id_start_date_type_unique
    on accounting.subscriptions(member_id, start_date, type);
