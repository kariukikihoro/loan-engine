package com.interview.loanengine.payment.dto;

import com.interview.loanengine.calculations.AdvanceInstallmentResult;
import com.interview.loanengine.calculations.LoanCalculations;
import com.interview.loanengine.calculations.RescheduleResult;
import com.interview.loanengine.schedule.ScheduleResponse;
import com.interview.loanengine.transactionlogs.PrepaymentOption;
import com.interview.loanengine.transactionlogs.TransactionLog;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record PrepaymentRequest(
        String id,
        @NotNull PrepaymentOption option,
        @NotNull @Min(1) Integer installmentNumber,
        @NotNull @Positive BigDecimal amount,
        BigDecimal newEmi,
        Integer newTenor,
        BigDecimal newOutstanding,
        Integer installmentsFullyCovered,
        Integer nextPayableInstallment,
        BigDecimal remainingDueOnNextInstallment,
        List<ScheduleResponse> schedule
) {
    /** Response mapper for the recalculating options (Reduce EMI / Reduce Tenor). */
    public static PrepaymentRequest reschedule(TransactionLog log, PrepaymentOption option,
                                               BigDecimal amount, RescheduleResult result) {
        return new PrepaymentRequest(
                log.getId(),
                option,
                result.schedule().isEmpty() ? null : result.schedule().get(0).getInstallmentNumber(),
                money(amount),
                money(result.newEmi()),
                result.newTenor(),
                money(result.newPrincipal()),
                null,
                null,
                null,
                result.schedule().stream().map(ScheduleResponse::from).toList());
    }

    /** Response mapper for the advance-installments option (no recalculation). */
    public static PrepaymentRequest advance(TransactionLog log, Integer installmentNumber,
                                            BigDecimal amount, AdvanceInstallmentResult result) {
        return new PrepaymentRequest(
                log.getId(),
                PrepaymentOption.ADVANCE_INSTALLMENTS,
                installmentNumber,
                money(amount),
                null,
                null,
                money(result.outstandingBeforePrepayment()),
                result.installmentsFullyCovered(),
                result.nextPayableInstallmentNumber(),
                money(result.remainingDueOnNextInstallment()),
                null);
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? null : LoanCalculations.toMonetaryValue(value);
    }
}
