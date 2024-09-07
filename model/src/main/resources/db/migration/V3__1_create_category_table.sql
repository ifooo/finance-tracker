create table personal_finance_tracker.category
(
    id         serial
        constraint category_pk primary key
        constraint category_pk_unique unique,
    name       varchar(255),
    created_at timestamp with time zone
);
