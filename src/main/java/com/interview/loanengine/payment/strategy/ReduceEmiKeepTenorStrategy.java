package com.interview.loanengine.payment.strategy;

import com.interview.loanengine.calculations.RescheduleResult;
import com.interview.loanengine.calculations.ScheduleOps;
import com.interview.loanengine.loan.Loan;
import com.interview.loanengine.loan.LoanRepository;
import com.interview.loanengine.payment.dto.PrepaymentRequest;
import com.interview.loanengine.schedule.Schedule;
import com.interview.loanengine.schedule.ScheduleRepository;
import com.interview.loanengine.transactionlogs.PrepaymentOption;
import com.interview.loanengine.transactionlogs.TransactionLog;
import org.springframework.stereotype.Component;

import java.util.List;

/** Option A: the prepayment lowers the EMI; the remaining tenor is unchanged. */
@Component
class ReduceEmiKeepTenorStrategy extends ReschedulingStrategySupport {

    ReduceEmiKeepTenorStrategy(ScheduleRepository scheduleRepository, LoanRepository loanRepository) {
        super(scheduleRepository, loanRepository);
    }

    @Override
    public PrepaymentOption option() {
        return PrepaymentOption.REDUCE_EMI_KEEP_TENOR;
    }

    @Override
    public PrepaymentRequest apply(Loan loan, List<Schedule> activeSchedule, PrepaymentRequest request, TransactionLog log) {
        RescheduleResult result = ScheduleOps.applyReduceEmiKeepTenor(
                loan, activeSchedule, request.installmentNumber(), request.amount());

        persistReschedule(activeSchedule, request.installmentNumber(), result, log);
        loan.setEquatedMonthlyInstallment(result.newEmi());
        updateLoanBalance(loan, result);

        return PrepaymentRequest.reschedule(log, option(), request.amount(), result);
    }
}
