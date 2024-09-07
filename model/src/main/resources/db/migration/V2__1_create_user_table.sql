create table personal_finance_tracker.user_account
(
    id    serial
        constraint user_account_pk primary key
        constraint user_account_pk_unique unique,
    name  varchar(255),
    email varchar(255)
);
