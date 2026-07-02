package com.interview.loanengine.calculations;

import com.interview.loanengine.schedule.Schedule;

import java.math.BigDecimal;
import java.util.List;

public record RescheduleResult(
        BigDecimal outstandingBeforePrepayment,
        BigDecimal newPrincipal,
        BigDecimal newEmi,
        int newTenor,
        List<Schedule> schedule
) {
}
