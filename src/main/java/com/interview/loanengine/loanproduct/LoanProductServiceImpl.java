package com.interview.loanengine.loanproduct;

import com.interview.loanengine.utilities.exceptions.ResourceNotFoundException;
import com.interview.loanengine.utilities.specification.SearchSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanProductServiceImpl implements LoanProductService {

    private final LoanProductRepository loanProductRepository;

    @Override
    @Transactional
    public LoanProductRequest createLoanProduct(LoanProductRequest request) {
        if (loanProductRepository.existsByProductName(request.productName())) {
            throw new IllegalArgumentException("A loan product named '" + request.productName() + "' already exists");
        }
        LoanProduct saved = loanProductRepository.save(request.toEntity());
        return LoanProductRequest.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanProductRequest findLoanProductById(String id) {
        return LoanProductRequest.from(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanProductRequest> searchLoanProducts(String name,
                                                       BigDecimal interestFrom, BigDecimal interestTo,
                                                       Integer tenureFrom, Integer tenureTo, Pageable pageable) {
        Specification<LoanProduct> specification = SearchSpecifications.<LoanProduct>like("productName", name)
                .and(SearchSpecifications.range("interestRate", interestFrom, interestTo))
                .and(SearchSpecifications.range("tenureInMonths", tenureFrom, tenureTo));

        return loanProductRepository.findAll(specification, pageable)
                .map(LoanProductRequest::from)
                .toList();
    }

    private LoanProduct findOrThrow(String id) {
        return loanProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found: " + id));
    }
}
