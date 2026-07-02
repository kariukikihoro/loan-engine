package com.interview.loanengine.loanproduct;

import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

public interface LoanProductService {

    LoanProductRequest createLoanProduct(LoanProductRequest request);

    LoanProductRequest findLoanProductById(String id);

    List<LoanProductRequest> searchLoanProducts(String name, BigDecimal interestFrom, BigDecimal interestTo,
                                                Integer tenureFrom, Integer tenureTo, Pageable pageable);
}
