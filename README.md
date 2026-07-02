# Loan Engine

A small Spring Boot service for managing reducing-balance loans — from defining a loan
product, to disbursing a loan and generating its repayment schedule, to recording payments
and handling early (pre-) payments.

## What it does

- **Loan products** — reusable templates (interest rate, tenure in months, first payment month)
  that loans are created from.
- **Loans** — created against a product. The engine works out the **EMI** (Equated Monthly
  Installment) and builds the full **repayment schedule** using the standard reducing-balance
  formula.
- **Payments** — record one or more scheduled installments as paid.
- **Prepayments** — pay a lump sum early and choose how the loan should adjust:
  - **Reduce EMI, keep tenure** — smaller monthly payments, same number of months.
  - **Reduce tenure, keep EMI** — same monthly payment, loan finishes sooner.
  - **Advance installments** — the lump sum simply pre-funds future installments (no recalculation).
- **Search** — look up products, loans, and schedules with optional filters (and pagination
  where it makes sense).
- **Transaction log** — every payment and prepayment is recorded.

## Tech stack

- Java 21, Spring Boot 3.5
- Spring Data JPA (MySQL in production, H2 for tests)
- Gradle, JUnit 5

## Running it

You need Java 21 and a MySQL instance (the schema is created automatically from
`src/main/resources/schema.sql`).

```bash
# start the app (defaults connect to jdbc:mysql://localhost:3306/loanengine as root/root)
./gradlew bootRun

# run the tests (uses an in-memory H2 database — no MySQL needed)
./gradlew test
```

Database connection can be overridden with environment variables:

| Variable      | Default                                        |
|---------------|------------------------------------------------|
| `DB_URL`      | `jdbc:mysql://localhost:3306/loanengine?...`   |
| `DB_USERNAME` | `root`                                         |
| `DB_PASSWORD` | `root`                                         |

## API at a glance

All responses are wrapped in a common envelope (`message`, `data`, `status`).

### Loan products — `/loan-products`

| Method & path                | What it does                          |
|------------------------------|---------------------------------------|
| `POST /create`               | Create a loan product                 |
| `GET  /find-by-id?id=`       | Fetch one product                     |
| `GET  /search`               | Search by name / interest / tenure (paginated via `page` & `size`) |

```json
// POST /loan-products/create
{
  "productName": "Home Loan",
  "productDescription": "Standard home loan",
  "tenureInMonths": 60,
  "interestRate": 12.0,
  "firstPaymentMonth": 7
}
```

### Loans — `/loans`

| Method & path                    | What it does                                 |
|----------------------------------|----------------------------------------------|
| `POST /create`                   | Create a loan (returns the schedule)         |
| `GET  /find-by-id?loanId=`       | Fetch a loan                                 |
| `GET  /loan-schedule?loanId=`    | Fetch a loan's repayment schedule            |
| `GET  /search`                   | Search by tenure / loaned-amount range       |

```json
// POST /loans/create
{
  "loanProductId": "<product-id>",
  "loanAmount": 1000000,
  "firstPaymentDate": "2026-01-01"
}
```

### Payments — `/loans/{loanId}`

| Method & path        | What it does                                   |
|----------------------|------------------------------------------------|
| `POST /payments`     | Record N scheduled installments as paid        |
| `POST /prepayments`  | Make an early lump-sum payment                 |

```json
// POST /loans/{loanId}/payments
{ "numberOfInstallments": 2 }

// POST /loans/{loanId}/prepayments
{
  "option": "REDUCE_EMI_KEEP_TENOR",   // or REDUCE_TENOR_KEEP_EMI | ADVANCE_INSTALLMENTS
  "installmentNumber": 12,
  "amount": 200000
}
```

### Schedules — `/schedules`

| Method & path   | What it does                                          |
|-----------------|-------------------------------------------------------|
| `GET /search`   | Search installments by loan / product (`page`, `size`)|

## How the numbers work

Money is handled with `BigDecimal` throughout. Intermediate balances are kept at high
precision and only rounded to 2 decimal places (HALF_UP) for display, so the generated
schedule matches a standard reducing-balance amortization table. See
`calculations/LoanCalculations.java` for the EMI formula and schedule/prepayment logic.
