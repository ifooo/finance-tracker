# Personal Finance Tracker

A local service for tracking household finances: yours and your family's.

- **In:** transactions are recorded through the REST API. A simple UI is planned.
- **Out:** the app keeps a spreadsheet per month and a CSV of all transactions up to date as data arrives. On the 2nd of each month it also writes a PDF report for the previous month. Reports cover the whole household, with a per-person split.

```
~/finance/
├── outbox/
│   ├── 2026-11-report.xlsx     ← one per month, rewritten whenever that month changes
│   ├── 2026-10-report.pdf      ← one per month, generated on the 2nd of the following month
│   └── transactions.csv        ← every transaction, rewritten on every change
└── backups/                    ← database dumps from local/backup.sh
```

The folders are created on first use. All paths can be changed (see [Configuration](#configuration)).

## Getting started

### Prerequisites

- JDK 25 (or let Gradle provision the toolchain)
- Docker, for the local database and for the tests

### 1. Start the database

```bash
docker compose -f local/docker-compose.yml up -d
```

This runs PostgreSQL on `localhost:5433` (database, user and password are all `pft`). On first start the app creates the `personal_finance_tracker` schema with Flyway and seeds the [default categories](#categories).

### 2. Run the application

```bash
./gradlew :application:bootRun
```

The API is at `http://localhost:8080` and Swagger UI at `http://localhost:8080/swagger-ui.html`.

### 3. Personal settings: who is in the household

Personal settings live outside the repository, so names never end up in git. Copy the example and edit it:

```bash
mkdir -p ~/.config/finance-tracker
cp local/finance-tracker.example.yml ~/.config/finance-tracker/application.yml
```

```yaml
pft:
  household:
    default-person: ivo        # used when a transaction doesn't say whose it is
    people:
      - username: ivo          # short id for the person of a transaction
        name: Ivo              # what reports show
      - username: ana
        name: Ana
```

People are created or updated at every startup. See [People](#people) for details. The same file can override any other setting, such as the folders.

## Recording transactions

Use `POST /api/v1/transactions`, or try it in Swagger UI:

```bash
curl -X POST localhost:8080/api/v1/transactions -H 'Content-Type: application/json' -d '{
  "type": "EXPENSE",
  "amount": 1250.00,
  "currency": "MKD",
  "description": "Weekly groceries",
  "categoryIds": [5],
  "datetime": "2026-11-15T10:00:00+01:00",
  "person": "marija"
}'
```

| Field | Required | Notes |
|---|---|---|
| `type` | yes | `EXPENSE` or `INCOME` |
| `amount` | yes | Always positive; `type` says whether money came in or went out |
| `currency` | yes | `MKD`, `EUR` or `USD` |
| `description` | yes | Free text |
| `categoryIds` | yes | Category ids (may be empty); list them with `GET /api/v1/categories` |
| `datetime` | yes | ISO date-time with offset |
| `person` | no | Whose transaction it is, see [People](#people); defaults to the household default |

## People

Every transaction belongs to one person in the household. People are defined under `pft.household.people` in your personal settings (see [Getting started](#3-personal-settings-who-is-in-the-household)):

- They are created or updated at startup, matched by `username`. Removing someone from the list does not delete them or their transactions.
- `person` can be the username (`ana`), the full name (`Ana Petrova`) or just the first name (`Ana`), ignoring case. If two people share a first name, use the username.
- A transaction without `person` belongs to `pft.household.default-person`, or to the first person in the list when that is not set. With no people configured, everything belongs to the account created on first start.
- An unknown name is rejected with a 400 error that lists who is known.

## Categories

A fresh database is seeded with 21 default categories (Salary, Groceries, Dining/Restaurants, Utilities, Rent/Mortgage, Transportation and so on) by `application/src/main/resources/db/seed/R__seed_default_categories.sql`. To change the defaults, edit that file. It reruns safely: existing names are kept and nothing is deleted. You can also add categories with `POST /api/v1/categories`.

## Reports

Everything is written to `~/finance/outbox`. Reports cover the whole household. When more than one person has transactions in a month, they also show a per-person split. Amounts are always **per currency**: EUR, USD and MKD are never added together.

### Monthly spreadsheet: `<yyyy-MM>-report.xlsx`

Rewritten right after any change to that month is saved, for example a new or deleted transaction. At startup, the current and previous month are also rewritten, to pick up anything changed while the app was off.

| Sheet | Contents |
|---|---|
| Summary | Income, expenses and net per currency compared with the previous month, then the same per person |
| Transactions | Every transaction of the month with its person, with filters and a signed-amount column |
| By category | Expenses per category and currency, with share of the total and one column per person |
| Items | Receipt line items |

### All transactions: `transactions.csv`

Every transaction, oldest first, rewritten together with the spreadsheets. Use it for your own analysis in a spreadsheet, pandas and so on.

- Columns: `id, date, datetime, person, type, amount, signed_amount, currency, categories, merchant, description, payment_method, source, notes`.
- `signed_amount` is negative for expenses, so summing it per currency gives the net.
- When a transaction has several categories, they are joined with `; `.
- The file is UTF-8 with a byte-order mark, so Excel shows Cyrillic text correctly. With pandas, use `encoding="utf-8-sig"`. Fields are comma-separated with dot decimals, quoted per RFC 4180.

### Monthly PDF: `<yyyy-MM>-report.pdf`

A snapshot of last month, generated once from the 2nd of the month. The gap leaves a day to add late receipts. The check runs daily at 08:00 and at startup, so a run missed while the machine was off is caught up. Generated months are recorded in the `report_run` table.

The A4 report contains:
- an overview per currency with the change from last month;
- a per-person table;
- category bars with each person's share;
- top merchants;
- the full transaction list showing who spent what, with page numbers.

The per-person parts are left out when only one person has transactions that month.

It embeds Liberation Sans, so Cyrillic names render correctly. The layout lives in `service/src/main/resources/templates/monthly-report-pdf.ftl`.

### On demand

A transaction added after the PDF was made still updates the spreadsheet and CSV automatically, but not the PDF. To rebuild the PDF:

```bash
curl 'localhost:8080/api/v1/reports/monthly?month=2026-10'                # JSON summary, no side effects
curl -X POST 'localhost:8080/api/v1/reports/monthly/files?month=2026-10'   # rewrite that month's .xlsx and .pdf now
```

## API overview

All endpoints are under `/api/v1`. The app is meant to run locally only, so there is no authentication.

| Resource | Endpoints |
|---|---|
| `transactions` | `POST` create (optional `person`) · `GET` list (paged; `type`=`INCOME`/`EXPENSE`, optional `from`/`to`) · `DELETE /{id}` |
| `categories` | `POST` create · `GET` list (paged) · `DELETE /{id}` |
| `reports` | `GET /monthly?month=yyyy-MM` · `POST /monthly/files?month=yyyy-MM` |

Paged endpoints accept `page` (default `0`) and `size` (default `10`).

## Configuration

Settings live under `pft.*` in `application/src/main/resources/application.yml`. You can override them in `~/.config/finance-tracker/application.yml`.

| Key | Default | Meaning |
|---|---|---|
| `pft.household.people` | *(none)* | Household members: `username`, `name`, optional `email` |
| `pft.household.default-person` | first person | Username used when a transaction has no `person` |
| `pft.reports.outbox-dir` | `~/finance/outbox` | Where the spreadsheets, CSV and PDFs are written |
| `pft.reports.live-spreadsheet` | `true` | Rewrite the monthly spreadsheets and `transactions.csv` whenever data changes |
| `pft.reports.pdf.enabled` | `true` | Generate the monthly PDF |
| `pft.reports.pdf.day-of-month` | `2` | From this day on, last month's PDF is generated (1–28) |
| `pft.reports.pdf.check-cron` | `0 0 8 * * *` | When the daily PDF check runs |

Month boundaries in reports use the machine's time zone.

## Backups

```bash
local/backup.sh   # pg_dump to ~/finance/backups/pft-<timestamp>.dump, keeps the 30 newest
```

Restore into an empty database:

```bash
docker exec -i local-pft-db-1 pg_restore -U pft -d pft --clean --if-exists < ~/finance/backups/pft-<timestamp>.dump
```

## Troubleshooting

- **A report file looks stale:** check the log for `Report: …` lines. Every write is logged, and so are failures.
- **The wrong people are listed:** people come from `~/.config/finance-tracker/application.yml`, which is only read at startup, so restart after editing it.

## Development

### Tech stack

- Java 25, Spring Boot 4.1, Gradle (Kotlin DSL, wrapper included)
- PostgreSQL 16 with Flyway migrations
- Spring Data JPA, FreeMarker, Apache POI (Excel), Flying Saucer + OpenPDF (PDF)
- springdoc-openapi (Swagger UI)
- JUnit 5 + Testcontainers

### Project structure

| Path | Purpose |
|---|---|
| `application` | Spring Boot entry point, `application.yml`, default-category seed |
| `api` | REST controllers, response mappers, global exception handling |
| `service` | Business logic: reports and exports (`report`), people (`person`), transactions, categories |
| `model` | JPA entities, repositories, enums and Flyway migrations |
| `local` | Docker Compose for the database, backup script, example personal settings |

### Run the tests

```bash
./gradlew test
```

Integration tests start their own PostgreSQL container through Testcontainers, so Docker must be running. Tests never touch `~/finance` and ignore your personal settings file.
