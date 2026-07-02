package com.interview.loanengine.payment.dto;

import com.interview.loanengine.calculations.LoanCalculations;
import com.interview.loanengine.schedule.Schedule;
import com.interview.loanengine.schedule.ScheduleResponse;
import com.interview.loanengine.transactionlogs.TransactionLog;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record InstallmentPaymentRequest(
        String id,
        @NotNull @Min(1) Integer numberOfInstallments,
        List<ScheduleResponse> paidInstallments,
        BigDecimal outstandingBalance
) {
    public static InstallmentPaymentRequest fromTransactionLog(TransactionLog log, List<Schedule> paidInstallments,
                                                               BigDecimal outstandingBalance) {
        return new InstallmentPaymentRequest(
                log.getId(),
                paidInstallments.size(),
                paidInstallments.stream().map(ScheduleResponse::from).toList(),
                outstandingBalance == null ? null : LoanCalculations.toMonetaryValue(outstandingBalance));
    }
}
