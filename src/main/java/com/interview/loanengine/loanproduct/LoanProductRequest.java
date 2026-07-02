package com.interview.loanengine.loanproduct;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LoanProductRequest(
        String id,
        @NotBlank String productName,
        @NotBlank String productDescription,
        @NotNull @Min(1) Integer tenureInMonths,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal interestRate,
        @NotNull @Min(1) @Max(12) Integer firstPaymentMonth
) {
    public LoanProduct toEntity() {
        return LoanProduct.builder()
                .productName(productName)
                .productDescription(productDescription)
                .tenureInMonths(tenureInMonths)
                .interestRate(interestRate)
                .firstPaymentMonth(firstPaymentMonth)
                .build();
    }

    public static LoanProductRequest from(LoanProduct product) {
        return new LoanProductRequest(
                product.getId(),
                product.getProductName(),
                product.getProductDescription(),
                product.getTenureInMonths(),
                product.getInterestRate(),
                product.getFirstPaymentMonth());
    }
}
