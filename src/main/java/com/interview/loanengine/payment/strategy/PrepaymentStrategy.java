package com.interview.loanengine.payment.strategy;

import com.interview.loanengine.loan.Loan;
import com.interview.loanengine.payment.dto.PrepaymentRequest;
import com.interview.loanengine.schedule.Schedule;
import com.interview.loanengine.transactionlogs.PrepaymentOption;
import com.interview.loanengine.transactionlogs.TransactionLog;

import java.util.List;

/**
 * One implementation per {@link PrepaymentOption}, keeping option-specific calculation and
 * persistence out of the payment service.
 */
public interface PrepaymentStrategy {

    PrepaymentOption option();

    PrepaymentRequest apply(Loan loan, List<Schedule> activeSchedule, PrepaymentRequest request, TransactionLog log);
}
