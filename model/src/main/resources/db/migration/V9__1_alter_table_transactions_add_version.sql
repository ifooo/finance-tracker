alter table personal_finance_tracker.transaction
    add version integer not null default 0;

alter table personal_finance_tracker.transaction
    add updated_at timestamp with time zone;

alter table personal_finance_tracker.budget
    add version integer not null default 0;

alter table personal_finance_tracker.budget
    add updated_at timestamp with time zone;

alter table personal_finance_tracker.category
    add version integer not null default 0;

alter table personal_finance_tracker.category
    add updated_at timestamp with time zone;

alter table personal_finance_tracker.goal
    add version integer not null default 0;

alter table personal_finance_tracker.goal
    add updated_at timestamp with time zone;
