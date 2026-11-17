# Personal Finance Tracker

A local service with no UI for tracking household finances: yours and your family's.

- **In:** drop hand-written JSON files into an inbox folder.
- **Out:** the app keeps a spreadsheet per month and a CSV of all transactions up to date as data arrives. On the 2nd of each month it also writes a PDF report for the previous month. Reports cover the whole household, with a per-person split.

```
~/finance/
├── inbox/                      ← drop *.json files here
│   ├── processed/yyyy/MM/      ← imported files end up here
│   └── failed/                 ← rejected files, each with a .error.txt explaining why
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
      - username: ivo          # what you write in inbox files
        name: Ivo              # what reports show
      - username: ana
        name: Ana
```

People are created or updated at every startup. See [People](#people) for details. The same file can override any other setting, such as the folders.

## Recording transactions: the inbox

Drop `*.json` files into `~/finance/inbox`. The folder is checked every 30 seconds:

| Result | What happens |
|---|---|
| Imported | The file moves to `processed/yyyy/MM/<timestamp>_<name>.json` |
| Already imported (same content) | The file moves to `processed/` and nothing is stored twice (matched by the SHA-256 of the content) |
| Invalid or unsupported | The file moves to `failed/` next to a `<name>.error.txt` listing every problem. **Nothing** from the file is stored |

A file is all-or-nothing: fix the errors and drop it in again. Files changed in the last 5 seconds, hidden files and partial downloads (`*.part`, `*.tmp`, `*.crdownload`) are left alone until they are complete.

### File format

```json
{
  "$schema": "./transactions.schema.json",
  "transactions": [
    {
      "externalId": "2026-11-15-vero",
      "type": "EXPENSE",
      "date": "2026-11-15",
      "amount": 1250.00,
      "currency": "MKD",
      "merchant": "Vero",
      "description": "Weekly groceries",
      "categories": ["Groceries"],
      "paymentMethod": "CARD",
      "notes": "optional",
      "items": [
        { "name": "Milk 1l", "quantity": 2, "unitPrice": 65.00 },
        { "name": "Bread", "total": 45.00 }
      ]
    }
  ]
}
```

| Field | Required | Notes |
|---|---|---|
| `type` | yes | `EXPENSE` or `INCOME` (case-insensitive) |
| `date` | yes | `yyyy-MM-dd` (start of that day, local time) or an ISO date-time such as `2026-11-15T18:30:00+01:00` |
| `amount` | yes | Always positive, at most 2 decimals; `type` says whether money came in or went out |
| `currency` | yes | `MKD`, `EUR` or `USD` |
| `categories` | yes | One or more category names, see [Categories](#categories) |
| `person` | no | Whose transaction it is, see [People](#people). Defaults to the file-level `person`, then to the household default |
| `externalId` | no | Your own unique id; an entry whose id was already imported is rejected, so a receipt is never stored twice |
| `merchant` | no | Shop or payer; used for the "top merchants" ranking |
| `description`, `notes` | no | Free text |
| `paymentMethod` | no | `CASH`, `CARD`, `BANK_TRANSFER` or `OTHER` |
| `items` | no | Receipt lines: `name`, plus either `total` or both `quantity` and `unitPrice` |

One file can hold any number of transactions. Misspelled field names (`"ammount"`) are rejected rather than silently ignored.

To record someone else's receipts, set `person` once at the top of the file. An entry can still override it:

```json
{
  "person": "ana",
  "transactions": [
    { "type": "EXPENSE", "date": "2026-11-16", "amount": 890, "currency": "MKD", "merchant": "Zegin", "categories": ["Health"] },
    { "person": "ivo", "type": "EXPENSE", "date": "2026-11-16", "amount": 18.50, "currency": "EUR", "categories": ["Dining"] }
  ]
}
```

**Editor support:** put `docs/inbox/transactions.schema.json` next to your files to get autocomplete and validation in VS Code or IntelliJ. `docs/inbox/example.json` is a complete example to copy.

## People

Every transaction belongs to one person in the household. People are defined under `pft.household.people` in your personal settings (see [Getting started](#3-personal-settings-who-is-in-the-household)):

- They are created or updated at startup, matched by `username`. Removing someone from the list does not delete them or their transactions.
- In inbox files and the API, `person` can be the username (`ana`), the full name (`Ana Petrova`) or just the first name (`Ana`), ignoring case. If two people share a first name, use the username.
- A transaction without `person` belongs to `pft.household.default-person`, or to the first person in the list when that is not set. With no people configured, everything belongs to the account created on first start.
- An unknown name rejects the file, and the error lists who is known.

## Categories

A fresh database is seeded with 21 default categories (Salary, Groceries, Dining/Restaurants, Utilities, Rent/Mortgage, Transportation and so on) by `application/src/main/resources/db/seed/R__seed_default_categories.sql`. To change the defaults, edit that file. It reruns safely: existing names are kept and nothing is deleted. You can also add categories with `POST /api/v1/categories`.

In inbox files, categories are matched by name, ignoring case, and short forms work too:

- `"Utilities"` matches *Utilities (Electricity, Water, Gas)* (the part before the parentheses).
- `"Restaurants"` or `"Dining"` matches *Dining/Restaurants* (either side of the slash).

Unknown names are rejected, so typos are caught, and the error file lists every known category. Set `pft.inbox.auto-create-categories: true` to create unknown categories instead.

## Reports

Everything is written to `~/finance/outbox`. Reports cover the whole household. When more than one person has transactions in a month, they also show a per-person split. Amounts are always **per currency**: EUR, USD and MKD are never added together.

### Monthly spreadsheet: `<yyyy-MM>-report.xlsx`

Rewritten right after any change to that month is saved, whether it comes from an inbox file or the API. At startup, the current and previous month are also rewritten, to pick up anything changed while the app was off.

| Sheet | Contents |
|---|---|
| Summary | Income, expenses and net per currency compared with the previous month, then the same per person |
| Transactions | Every transaction of the month with its person, with filters and a signed-amount column |
| By category | Expenses per category and currency, with share of the total and one column per person |
| Items | Receipt line items |

### All transactions: `transactions.csv`

Every transaction, oldest first, rewritten together with the spreadsheets. Use it for your own analysis in a spreadsheet, pandas and so on.

- Columns: `id, date, datetime, person, type, amount, signed_amount, currency, categories, merchant, description, payment_method, source, external_id, notes`.
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

A receipt that arrives after the PDF was made still updates the spreadsheet and CSV automatically, but not the PDF. To rebuild the PDF:

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
| `pft.inbox.enabled` | `true` | Poll the inbox |
| `pft.inbox.dir` | `~/finance/inbox` | Inbox folder |
| `pft.inbox.poll-interval` | `PT30S` | Time between scans |
| `pft.inbox.settle-time` | `PT5S` | Skip files modified more recently than this |
| `pft.inbox.auto-create-categories` | `false` | Create unknown categories instead of rejecting the file |
| `pft.reports.outbox-dir` | `~/finance/outbox` | Where the spreadsheets, CSV and PDFs are written |
| `pft.reports.live-spreadsheet` | `true` | Rewrite the monthly spreadsheets and `transactions.csv` whenever data changes |
| `pft.reports.pdf.enabled` | `true` | Generate the monthly PDF |
| `pft.reports.pdf.day-of-month` | `2` | From this day on, last month's PDF is generated (1–28) |
| `pft.reports.pdf.check-cron` | `0 0 8 * * *` | When the daily PDF check runs |

Plain dates in inbox files and month boundaries in reports use the machine's time zone.

## Backups

```bash
local/backup.sh   # pg_dump to ~/finance/backups/pft-<timestamp>.dump, keeps the 30 newest
```

Restore into an empty database:

```bash
docker exec -i local-pft-db-1 pg_restore -U pft -d pft --clean --if-exists < ~/finance/backups/pft-<timestamp>.dump
```

## Troubleshooting

- **A file landed in `failed/`:** open the `.error.txt` next to it. Each line names the entry and field, for example `transactions[2].categories: unknown category "Groceris"`. Fix the file and drop it into the inbox again.
- **A file stays in the inbox:** it is probably still within the 5-second settle time, or it is hidden or named like a partial download.
- **Something else:** the app logs every import (`Inbox: …`) and every file it writes (`Report: …`).

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
| `service` | Business logic: inbox import (`inbox`), reports and exports (`report`), people (`person`), transactions, categories |
| `model` | JPA entities, repositories, enums and Flyway migrations |
| `docs/inbox` | JSON Schema and example for inbox files |
| `local` | Docker Compose for the database, backup script, example personal settings |

### Run the tests

```bash
./gradlew test
```

Integration tests start their own PostgreSQL container through Testcontainers, so Docker must be running. Tests never touch `~/finance`.
