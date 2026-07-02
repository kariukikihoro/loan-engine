package com.interview.loanengine.transactionlogs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionLogRepository extends JpaRepository<TransactionLog, String> {

    List<TransactionLog> findByLoanIdOrderByTransactionDateAsc(String loanId);
}
