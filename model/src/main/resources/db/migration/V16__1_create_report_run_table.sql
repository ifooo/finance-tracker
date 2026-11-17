-- One row per month whose PDF report was generated, so a missed run (machine off) is caught up later.
create table personal_finance_tracker.report_run
(
    id           serial
        constraint report_run_pk primary key,
    month        varchar(7)               not null
        constraint report_run_month_unique unique,
    generated_at timestamp with time zone not null,
    created_at   timestamp with time zone not null,
    updated_at   timestamp with time zone not null,
    version      integer                  not null default 0
);
