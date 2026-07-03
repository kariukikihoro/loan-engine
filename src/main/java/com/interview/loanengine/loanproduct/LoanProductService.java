package com.interview.loanengine.loanproduct;

import com.interview.loanengine.utilities.PageResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface LoanProductService {

    LoanProductRequest createLoanProduct(LoanProductRequest request);

    LoanProductRequest findLoanProductById(String id);

    PageResponse<LoanProductRequest> searchLoanProducts(String name, BigDecimal interestFrom, BigDecimal interestTo,
                                                        Integer tenureFrom, Integer tenureTo, Pageable pageable);
}
