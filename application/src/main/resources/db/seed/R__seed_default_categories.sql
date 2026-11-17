-- Default categories for a fresh database. Repeatable and idempotent: edit this list to add more;
-- existing categories (matched case-insensitively) are left untouched, nothing is ever deleted.
insert into personal_finance_tracker.category (name, created_at, updated_at)
values ('Salary', now(), now()),
       ('Bonuses', now(), now()),
       ('Freelance/Contract', now(), now()),
       ('Gifts', now(), now()),
       ('Groceries', now(), now()),
       ('Dining/Restaurants', now(), now()),
       ('Utilities (Electricity, Water, Gas)', now(), now()),
       ('Rent/Mortgage', now(), now()),
       ('Transportation (Gas, Public Transit)', now(), now()),
       ('Entertainment', now(), now()),
       ('Health/Medical', now(), now()),
       ('Insurance (Health, Car, Home)', now(), now()),
       ('Subscriptions', now(), now()),
       ('Shopping (Clothes, Electronics)', now(), now()),
       ('Personal Care (Salon, Spa)', now(), now()),
       ('Education (Tuition, Books)', now(), now()),
       ('Childcare', now(), now()),
       ('Debt Repayment (Credit Card, Loan)', now(), now()),
       ('Savings/Investments', now(), now()),
       ('Travel/Vacation', now(), now()),
       ('Miscellaneous', now(), now())
on conflict (lower(name)) do nothing;
