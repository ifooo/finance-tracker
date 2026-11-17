-- Budgets and goals are out of scope for now: the app tracks expenses only.
drop table if exists personal_finance_tracker.budget;
drop table if exists personal_finance_tracker.goal;

-- People in the household. 'username' is the short id used in inbox files ("person": "ivo").
alter table personal_finance_tracker.user_account
    add username   varchar(50),
    add created_at timestamp with time zone,
    add updated_at timestamp with time zone,
    add version    integer not null default 0;

-- The account seeded by V8 gets its first name as username, so it matches a configured "ivo".
update personal_finance_tracker.user_account
set username   = lower(split_part(trim(name), ' ', 1)),
    created_at = now(),
    updated_at = now()
where username is null;

alter table personal_finance_tracker.user_account
    alter column username set not null,
    alter column created_at set not null,
    alter column updated_at set not null;

create unique index user_account_username_unique
    on personal_finance_tracker.user_account (lower(username));

-- V8 inserted id 1 explicitly; move the sequence past it so new people get fresh ids.
select setval(pg_get_serial_sequence('personal_finance_tracker.user_account', 'id'),
              coalesce((select max(id) from personal_finance_tracker.user_account), 0) + 1, false);
