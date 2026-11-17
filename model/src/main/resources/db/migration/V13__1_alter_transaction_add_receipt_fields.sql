alter table personal_finance_tracker.transaction
    add merchant       varchar(255),
    add payment_method varchar(50),
    add notes          text,
    add source         varchar(20) not null default 'API',
    add external_id    varchar(100),
    add import_file_id bigint
        constraint transaction_import_file_id_fk
            references personal_finance_tracker.import_file (id)
            on delete set null;

-- Optional client-supplied id; prevents the same entry from being imported twice.
create unique index transaction_external_id_unique
    on personal_finance_tracker.transaction (external_id)
    where external_id is not null;

create index transaction_date_from_idx
    on personal_finance_tracker.transaction (date_from);
