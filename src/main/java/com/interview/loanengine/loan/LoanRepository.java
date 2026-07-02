package com.interview.loanengine.loan;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LoanRepository extends JpaRepository<Loan, String>, JpaSpecificationExecutor<Loan> {
}
