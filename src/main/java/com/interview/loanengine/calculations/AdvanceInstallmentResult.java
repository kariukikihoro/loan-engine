package com.interview.loanengine.calculations;

import java.math.BigDecimal;

public record AdvanceInstallmentResult(
        BigDecimal outstandingBeforePrepayment,
        BigDecimal prepaymentAmount,
        int installmentsFullyCovered,
        int nextPayableInstallmentNumber,
        BigDecimal remainingDueOnNextInstallment
) {
}
