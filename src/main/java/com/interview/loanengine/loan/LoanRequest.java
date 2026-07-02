package com.interview.loanengine.loan;

import com.interview.loanengine.calculations.LoanCalculations;
import com.interview.loanengine.loanproduct.LoanProduct;
import com.interview.loanengine.schedule.Schedule;
import com.interview.loanengine.schedule.ScheduleResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LoanRequest(
        String id,
        @NotBlank String loanProductId,
        @NotNull @Positive BigDecimal loanAmount,
        @NotNull LocalDate firstPaymentDate,
        BigDecimal equatedMonthlyInstallment,
        Integer tenure,
        BigDecimal outstandingBalance,
        List<ScheduleResponse> schedule
) {
    /**
     * Build a new {@link Loan} entity from this request and its resolved product, deriving the
     * EMI and total interest from the product's rate and tenure.
     */
    public static Loan toEntity(LoanRequest request, LoanProduct product) {
        BigDecimal monthlyRate = LoanCalculations.convertToMonthlyRate(product.getInterestRate());
        BigDecimal emi = LoanCalculations.calculateEquatedMonthlyInstallment(
                request.loanAmount(), monthlyRate, product.getTenureInMonths());
        BigDecimal totalInterest = LoanCalculations.toMonetaryValue(
                emi.multiply(BigDecimal.valueOf(product.getTenureInMonths())).subtract(request.loanAmount()));

        return Loan.builder()
                .loanedAmount(request.loanAmount())
                .principalAmount(request.loanAmount())
                .interestAmount(totalInterest)
                .outstandingBalance(request.loanAmount())
                .equatedMonthlyInstallment(emi)
                .tenure(product.getTenureInMonths())
                .loanProduct(product)
                .build();
    }

    public static LoanRequest fromLoan(Loan loan, List<Schedule> schedule) {
        return new LoanRequest(
                loan.getId(),
                loan.getLoanProduct() == null ? null : loan.getLoanProduct().getId(),
                money(loan.getLoanedAmount()),
                null,
                money(loan.getEquatedMonthlyInstallment()),
                loan.getTenure(),
                money(loan.getOutstandingBalance()),
                schedule.stream().map(ScheduleResponse::from).toList());
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? null : LoanCalculations.toMonetaryValue(value);
    }
}
