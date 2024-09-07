create table personal_finance_tracker.goal
(
    id             serial
        constraint goal_pk
            primary key
        constraint goal_pk2
            unique,
    user_id        serial
        constraint goal_user_account_id_fk
            references personal_finance_tracker.user_account (id),
    name           varchar(255),
    target_amount  numeric(19,2),
    current_amount numeric(19,2),
    deadline       timestamp with time zone,
    created_at     timestamp with time zone
);

