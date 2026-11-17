-- Optional receipt line items.
create table personal_finance_tracker.transaction_item
(
    id             serial
        constraint transaction_item_pk primary key,
    transaction_id bigint         not null
        constraint transaction_item_transaction_id_fk
            references personal_finance_tracker.transaction (id)
            on delete cascade,
    name           varchar(255)   not null,
    quantity       numeric(19, 3),
    unit_price     numeric(19, 2),
    total          numeric(19, 2) not null
);

create index transaction_item_transaction_id_idx
    on personal_finance_tracker.transaction_item (transaction_id);
