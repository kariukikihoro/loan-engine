package com.interview.loanengine.schedule;

public enum InstallmentStatus {
    PENDING,
    PAID,
    ADJUSTED   // superseded by a prepayment/settlement recalculation
}
