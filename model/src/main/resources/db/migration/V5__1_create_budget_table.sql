create table personal_finance_tracker.budget
(
    id            serial
        constraint budget_pk primary key
        constraint budget_pk_unique unique,
    user_id       serial
        constraint budget_user_account_id_fk
            references personal_finance_tracker.user_account (id),
    category_id   serial
        constraint budget_category_id_fk
            references personal_finance_tracker.category (id),
    amount        numeric(19,2),
    budget_period varchar(255),
    start_date    timestamp with time zone,
    end_date      timestamp with time zone,
    created_at    timestamp with time zone
);