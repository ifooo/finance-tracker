alter table personal_finance_tracker.transaction
drop constraint transaction_category_id_fk;

alter table personal_finance_tracker.transaction
drop column category_id;

