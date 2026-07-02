package com.interview.loanengine.calculations;

import com.interview.loanengine.loan.Loan;
import com.interview.loanengine.loanproduct.LoanProduct;
import com.interview.loanengine.schedule.Schedule;
import com.interview.loanengine.utilities.exceptions.InvalidPrepaymentException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Verifies the base amortization schedule and the Category A / Option A
 * (Reduce EMI, Keep Tenor) recalculation against the provided sample loan data,
 * plus Options B and C. EMI/rate math itself is exercised in {@link LoanCalculationsTest}.
 */
class ScheduleOpsTest {

    private static final LocalDate FIRST_PAYMENT = LocalDate.of(2024, 7, 24);

    private Loan baseLoan() {
        LoanProduct product = LoanProduct.builder()
                .interestRate(new BigDecimal("12"))
                .tenureInMonths(60)
                .build();

        BigDecimal emi = LoanCalculations.calculateEquatedMonthlyInstallment(
                new BigDecimal("1000000"),
                LoanCalculations.convertToMonthlyRate(product.getInterestRate()),
                product.getTenureInMonths());

        return Loan.builder()
                .loanedAmount(new BigDecimal("1000000"))
                .equatedMonthlyInstallment(emi)
                .loanProduct(product)
                .build();
    }

    private static BigDecimal money(BigDecimal value) {
        return LoanCalculations.toMonetaryValue(value);
    }

    @Test
    void baseScheduleMatchesSampleData() {
        Loan loan = baseLoan();
        List<Schedule> schedule = ScheduleOps.generateSchedule(loan, FIRST_PAYMENT);

        assertEquals(60, schedule.size());
        assertEquals(new BigDecimal("22244.45"), money(loan.getEquatedMonthlyInstallment()));

        Schedule first = schedule.get(0);
        assertEquals(new BigDecimal("10000.00"), money(first.getInterest()));
        assertEquals(new BigDecimal("12244.45"), money(first.getPrincipalAmount()));

        // Outstanding principal owed entering installment 24 (after 23 fromTransactionLog).
        assertEquals(new BigDecimal("685118.09"), money(schedule.get(22).getPrincipalBalance()));
    }

    @Test
    void optionAReduceEmiKeepTenor() {
        Loan loan = baseLoan();
        List<Schedule> base = ScheduleOps.generateSchedule(loan, FIRST_PAYMENT);

        RescheduleResult result = ScheduleOps.applyReduceEmiKeepTenor(
                loan, base, 24, new BigDecimal("200000"));

        assertEquals(new BigDecimal("685118.09"), money(result.outstandingBeforePrepayment()));
        assertEquals(new BigDecimal("485118.09"), money(result.newPrincipal()));
        assertEquals(new BigDecimal("16112.86"), money(result.newEmi()));
        assertEquals(36, result.newTenor());
        assertEquals(36, result.schedule().size());

        Schedule firstNew = result.schedule().get(0);
        assertEquals(25, firstNew.getInstallmentNumber());
        assertEquals(LocalDate.of(2026, 7, 24), firstNew.getScheduledDate());
        assertEquals(new BigDecimal("4851.18"), money(firstNew.getInterest()));
        assertEquals(new BigDecimal("11261.68"), money(firstNew.getPrincipalAmount()));

        Schedule lastNew = result.schedule().get(35);
        assertEquals(60, lastNew.getInstallmentNumber());
        assertEquals(0, lastNew.getPrincipalBalance().signum(), "loan must be fully settled");
    }

    @Test
    void optionBReduceTenorKeepEmi() {
        Loan loan = baseLoan();
        List<Schedule> base = ScheduleOps.generateSchedule(loan, FIRST_PAYMENT);

        RescheduleResult result = ScheduleOps.applyReduceTenorKeepEmi(
                loan, base, 24, new BigDecimal("200000"));

        assertEquals(new BigDecimal("485118.09"), money(result.newPrincipal()));
        // EMI is unchanged fromLoan the original loan.
        assertEquals(money(loan.getEquatedMonthlyInstallment()), money(result.newEmi()));
        // 24 full installments (25..48) plus one smaller final installment (49).
        assertEquals(25, result.newTenor());

        Schedule firstNew = result.schedule().get(0);
        assertEquals(25, firstNew.getInstallmentNumber());
        assertEquals(money(loan.getEquatedMonthlyInstallment()), money(firstNew.getEmiAmount()));

        Schedule lastNew = result.schedule().get(result.schedule().size() - 1);
        assertEquals(49, lastNew.getInstallmentNumber());
        assertEquals(0, lastNew.getPrincipalBalance().signum(), "loan must be fully settled");
        // Final installment is a partial payment smaller than the standard EMI.
        assertEquals(-1, money(lastNew.getEmiAmount()).compareTo(money(loan.getEquatedMonthlyInstallment())));
    }

    @Test
    void optionCAdvanceInstallments() {
        Loan loan = baseLoan();
        List<Schedule> base = ScheduleOps.generateSchedule(loan, FIRST_PAYMENT);

        AdvanceInstallmentResult result = ScheduleOps.applyAdvanceInstallments(
                loan, base, 24, new BigDecimal("200000"));

        // 200,000 covers installments 24..31 in full, leaving a small out-of-pocket on 32.
        assertEquals(8, result.installmentsFullyCovered());
        assertEquals(32, result.nextPayableInstallmentNumber());
        assertEquals(new BigDecimal("200.03"), money(result.remainingDueOnNextInstallment()));
    }

    @Test
    void rejectsPrepaymentAtOrAboveOutstanding() {
        Loan loan = baseLoan();
        List<Schedule> base = ScheduleOps.generateSchedule(loan, FIRST_PAYMENT);

        assertThrows(InvalidPrepaymentException.class,
                () -> ScheduleOps.applyReduceEmiKeepTenor(loan, base, 24, new BigDecimal("700000")));
    }

    @Test
    void rejectsInvalidInputs() {
        Loan loan = baseLoan();
        List<Schedule> base = ScheduleOps.generateSchedule(loan, FIRST_PAYMENT);

        assertThrows(InvalidPrepaymentException.class,
                () -> ScheduleOps.applyReduceEmiKeepTenor(loan, base, 24, new BigDecimal("-1")));
        assertThrows(InvalidPrepaymentException.class,
                () -> ScheduleOps.applyReduceEmiKeepTenor(loan, base, 60, new BigDecimal("200000")));
    }
}