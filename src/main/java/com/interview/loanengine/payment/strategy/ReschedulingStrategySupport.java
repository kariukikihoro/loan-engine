package com.interview.loanengine.payment.strategy;

import com.interview.loanengine.calculations.RescheduleResult;
import com.interview.loanengine.loan.Loan;
import com.interview.loanengine.loan.LoanRepository;
import com.interview.loanengine.schedule.InstallmentStatus;
import com.interview.loanengine.schedule.Schedule;
import com.interview.loanengine.schedule.ScheduleRepository;
import com.interview.loanengine.transactionlogs.TransactionLog;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Shared persistence for the two recalculating options: supersede the remaining active
 * installments as ADJUSTED and persist the recalculated schedule as PENDING.
 */
@RequiredArgsConstructor
abstract class ReschedulingStrategySupport implements PrepaymentStrategy {

    protected final ScheduleRepository scheduleRepository;
    protected final LoanRepository loanRepository;

    protected void persistReschedule(List<Schedule> activeSchedule, int installmentNumber,
                                     RescheduleResult result, TransactionLog log) {
        List<Schedule> superseded = activeSchedule.stream()
                .filter(schedule -> schedule.getInstallmentNumber() >= installmentNumber)
                .toList();
        superseded.forEach(schedule -> {
            schedule.setStatus(InstallmentStatus.ADJUSTED);
            schedule.setTransactionLog(log);
        });
        scheduleRepository.saveAll(superseded);

        result.schedule().forEach(schedule -> {
            schedule.setStatus(InstallmentStatus.PENDING);
            schedule.setTransactionLog(log);
        });
        scheduleRepository.saveAll(result.schedule());
    }

    protected void updateLoanBalance(Loan loan, RescheduleResult result) {
        loan.setOutstandingBalance(result.newPrincipal());
        loanRepository.save(loan);
    }
}
