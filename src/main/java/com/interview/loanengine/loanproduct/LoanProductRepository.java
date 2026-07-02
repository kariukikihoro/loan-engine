package com.interview.loanengine.loanproduct;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LoanProductRepository extends JpaRepository<LoanProduct, String>,
        JpaSpecificationExecutor<LoanProduct> {

    boolean existsByProductName(String productName);
}
