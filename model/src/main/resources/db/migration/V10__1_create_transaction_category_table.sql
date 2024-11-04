create table personal_finance_tracker.transaction_category
(
    transaction_id serial
        constraint transaction_category_transaction_id_fk
            references personal_finance_tracker.transaction
            on delete cascade,
    category_id    serial
        constraint transaction_category_category_id_fk
            references personal_finance_tracker.category
            on delete cascade,
    constraint transaction_category_pk
        primary key (transaction_id, category_id)
);

