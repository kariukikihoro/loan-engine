# Loan Engine

A Spring Boot service for managing reducing-balance loans — from defining a loan product, to
disbursing a loan and generating its repayment schedule, to recording payments and handling
prepayments (Category A of the assessment: all three options).

## What it does

- **Loan products** — reusable templates (interest rate, tenure in months, first payment month)
  that loans are created from. Three products are seeded automatically: `prod-1` (Personal Loan,
  12 mo @ 12%), `prod-2` (Business Loan, 24 mo @ 15.5%), `prod-3` (Asset Financing Loan, 36 mo @ 10%).
- **Loans** — created against a product. The engine works out the **EMI** (Equated Monthly
  Installment) and builds the full **repayment schedule** using the standard reducing-balance
  formula. Loans carry a lifecycle status (`ACTIVE` / `CLOSED`).
- **Payments** — record one or more scheduled installments as paid. Paying the final pending
  installment closes the loan.
- **Prepayments** — pay a lump sum early at any valid installment and choose how the loan adjusts:
  - **Option A — Reduce EMI, keep tenor** (`REDUCE_EMI_KEEP_TENOR`): smaller monthly payments,
    same number of months.
  - **Option B — Reduce tenor, keep EMI** (`REDUCE_TENOR_KEEP_EMI`): same monthly payment, loan
    finishes sooner.
  - **Option C — Advance installments** (`ADVANCE_INSTALLMENTS`): no recalculation; the lump sum
    pre-funds whole future installments and any leftover is stored as a credit against the next one.
  Each option is implemented as a `PrepaymentStrategy`; superseded installments are marked
  `ADJUSTED` and the recalculated schedule is written alongside them, all in one transaction.
- **Transaction log** — every payment and prepayment is recorded immutably, including the
  business option applied.

## Tech stack

- Java 21, Spring Boot 3.5
- Spring Data JPA (MySQL in production, H2 for tests)
- Liquibase for schema migration and seed data
- Gradle, JUnit 5

## Running it

You need Java 21 and a MySQL instance. On startup, Liquibase creates the tables
(`src/main/resources/schema.sql`, executed via `db/changelog/db.changelog-master.xml`) and seeds
the three loan products — once, and only if no products exist yet.

```bash
# start the app (defaults connect to jdbc:mysql://localhost:3306/loanengine as user/userpassword)
./gradlew bootRun

# run the tests (uses an in-memory H2 database — no MySQL needed)
./gradlew test
```

Database connection can be overridden with environment variables:

| Variable      | Default                                        |
|---------------|------------------------------------------------|
| `DB_URL`      | `jdbc:mysql://localhost:3306/loanengine?...`   |
| `DB_USERNAME` | `user`                                         |
| `DB_PASSWORD` | `userpassword`                                 |

Interactive API docs (Swagger UI) are served at **http://localhost:8080/swagger-ui.html** —
every endpoint below can be triggered from there with pre-filled examples.

## Trying it with curl

```bash
# 1. Create a loan against a seeded product (1,000,000 over the product's tenure)
curl -s -X POST localhost:8080/loans/create \
  -H 'Content-Type: application/json' \
  -d '{"loanProductId":"prod-1","loanAmount":1000000,"firstPaymentDate":"2026-08-01"}'
# -> note the returned loan "id" (LOAN_ID below); the response includes the full schedule

# 2. Fetch the repayment schedule
curl -s "localhost:8080/loans/loan-schedule?loanId=LOAN_ID"

# 3. Pay the first two installments
curl -s -X POST localhost:8080/loans/LOAN_ID/payments \
  -H 'Content-Type: application/json' \
  -d '{"numberOfInstallments":2}'

# 4. Prepay 200,000 at installment 3 — choose the strategy via "option"
curl -s -X POST localhost:8080/loans/LOAN_ID/prepayments \
  -H 'Content-Type: application/json' \
  -d '{"option":"REDUCE_EMI_KEEP_TENOR","installmentNumber":3,"amount":200000}'
```

## API at a glance

All responses are wrapped in a common envelope (`message`, `object`, `status`).

### Loan products — `/loan-products`

| Method & path                | What it does                          |
|------------------------------|---------------------------------------|
| `POST /create`               | Create a loan product                 |
| `GET  /find-by-id?id=`       | Fetch one product                     |
| `GET  /search`               | Search by name / interest / tenure (paginated via `page` & `size`) |

### Loans — `/loans`

| Method & path                    | What it does                                 |
|----------------------------------|----------------------------------------------|
| `POST /create`                   | Create a loan (returns the schedule)         |
| `GET  /find-by-id?loanId=`       | Fetch a loan (includes status and balance)   |
| `GET  /loan-schedule?loanId=`    | Fetch a loan's repayment schedule            |
| `GET  /search`                   | Search by tenure / loaned-amount range       |

### Payments — `/loans/{loanId}`

| Method & path        | What it does                                   |
|----------------------|------------------------------------------------|
| `POST /payments`     | Record N scheduled installments as paid        |
| `POST /prepayments`  | Make an early lump-sum payment (options above) |

### Schedules — `/schedules`

| Method & path   | What it does                                          |
|-----------------|-------------------------------------------------------|
| `GET /search`   | Search installments by loan / product (`page`, `size`)|

## Validation & error handling

Structured JSON errors are returned for invalid operations, e.g. negative or zero amounts,
prepayments at or above the outstanding balance (use settlement instead), prepaying an
installment that is already paid, advance amounts exceeding the total remaining obligations, or
paying more installments than remain. Unknown loans/products return `404`.

## How the numbers work

Money is handled with `BigDecimal` throughout (`DECIMAL(23,10)` columns). Intermediate balances
are kept at 10-decimal precision and only rounded to 2 decimal places (HALF_UP) for presentation,
so the generated schedule matches the reference reducing-balance amortization table to the cent.
See `calculations/LoanCalculations.java` for the EMI formula and `calculations/ScheduleOps.java`
for schedule generation and the three prepayment computations.
