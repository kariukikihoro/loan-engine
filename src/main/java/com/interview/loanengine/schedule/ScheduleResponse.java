package com.interview.loanengine.schedule;

import com.interview.loanengine.calculations.LoanCalculations;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ScheduleResponse(
        Integer installmentNumber,
        LocalDate scheduledDate,
        BigDecimal principal,
        BigDecimal interest,
        BigDecimal amount,
        BigDecimal balance,
        InstallmentStatus status
) {
    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getInstallmentNumber(),
                schedule.getScheduledDate(),
                money(schedule.getPrincipalAmount()),
                money(schedule.getInterest()),
                money(schedule.getEmiAmount()),
                money(schedule.getPrincipalBalance()),
                schedule.getStatus());
    }

    private static BigDecimal money(BigDecimal value) {
        return value == null ? null : LoanCalculations.toMonetaryValue(value);
    }
}
