-- One row per successfully imported inbox file; sha256 makes re-dropped files idempotent.
create table personal_finance_tracker.import_file
(
    id                serial
        constraint import_file_pk primary key,
    file_name         varchar(512)             not null,
    sha256            varchar(64)              not null
        constraint import_file_sha256_unique unique,
    transaction_count integer                  not null default 0,
    imported_at       timestamp with time zone not null,
    created_at        timestamp with time zone not null,
    updated_at        timestamp with time zone not null,
    version           integer                  not null default 0
);
