-- Inbox files reference categories by name, so names must be unique regardless of case.
create unique index category_name_lower_unique
    on personal_finance_tracker.category (lower(name));
