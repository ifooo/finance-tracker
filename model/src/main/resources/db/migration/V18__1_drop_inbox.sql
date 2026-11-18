-- The JSON inbox was removed; transactions are entered through the API (and soon a UI).
update personal_finance_tracker.transaction
set source = 'API'
where source = 'INBOX_JSON';

drop index if exists personal_finance_tracker.transaction_external_id_unique;

alter table personal_finance_tracker.transaction
    drop column if exists external_id,
    drop column if exists import_file_id;

drop table if exists personal_finance_tracker.import_file;
