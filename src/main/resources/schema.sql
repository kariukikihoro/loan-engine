CREATE TABLE IF NOT EXISTS loan_product (
    id                  VARCHAR(36) PRIMARY KEY,
    deleted             BIT,
    created_at          TIMESTAMP,
    product_name        VARCHAR(255) NOT NULL UNIQUE,
    product_description VARCHAR(255) NOT NULL,
    tenure_in_months    INT NOT NULL,
    interest_rate       DECIMAL(23, 10) NOT NULL,
    first_payment_month INT NOT NULL
);

CREATE TABLE IF NOT EXISTS loan (
    id                          VARCHAR(36) PRIMARY KEY,
    deleted                     BIT,
    created_at                  TIMESTAMP,
    loaned_amount               DECIMAL(23, 10),
    principal_amount            DECIMAL(23, 10),
    interest_amount             DECIMAL(23, 10),
    outstanding_balance         DECIMAL(23, 10),
    equated_monthly_installment DECIMAL(23, 10),
    tenure                      INT,
    loan_product_id             VARCHAR(36),
    CONSTRAINT fk_loan_product FOREIGN KEY (loan_product_id) REFERENCES loan_product (id)
);

CREATE TABLE IF NOT EXISTS transaction_log (
    id                 VARCHAR(36) PRIMARY KEY,
    deleted            BIT,
    created_at         TIMESTAMP,
    transaction_date   DATE,
    transaction_type   VARCHAR(30),
    prepayment_option  VARCHAR(30),
    transaction_amount DECIMAL(23, 10),
    loan_id            VARCHAR(36),
    CONSTRAINT fk_transaction_loan FOREIGN KEY (loan_id) REFERENCES loan (id)
);

CREATE TABLE IF NOT EXISTS schedule (
    id                        VARCHAR(36) PRIMARY KEY,
    deleted                   BIT,
    created_at                TIMESTAMP,
    installment_number        INT,
    scheduled_date            DATE,
    principal_amount          DECIMAL(23, 10),
    interest                  DECIMAL(23, 10),
    emi_amount                DECIMAL(23, 10),
    running_balance           DECIMAL(23, 10),
    principal_running_balance DECIMAL(23, 10),
    status                    VARCHAR(20),
    loan_id                   VARCHAR(36),
    transaction_log_id        VARCHAR(36),
    CONSTRAINT fk_schedule_loan FOREIGN KEY (loan_id) REFERENCES loan (id),
    CONSTRAINT fk_schedule_transaction FOREIGN KEY (transaction_log_id) REFERENCES transaction_log (id)
);
