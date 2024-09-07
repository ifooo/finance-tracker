create table personal_finance_tracker.transaction
(
    id          serial
        constraint transaction_pk primary key
        constraint transaction_pk_unique unique,
    user_id     serial
        constraint transaction_user_account_id_fk
            references personal_finance_tracker.user_account (id),
    type        varchar(100),
    amount      numeric(19,2),
    description varchar(255),
    category_id serial
        constraint transaction_category_id_fk
            references personal_finance_tracker.category (id),
    date_from   timestamp with time zone,
    created_at  timestamp with time zone
);
