package com.interview.loanengine.payment.strategy;

import com.interview.loanengine.calculations.AdvanceInstallmentResult;
import com.interview.loanengine.calculations.ScheduleOps;
import com.interview.loanengine.loan.Loan;
import com.interview.loanengine.loan.LoanRepository;
import com.interview.loanengine.loan.LoanStatus;
import com.interview.loanengine.payment.dto.PrepaymentRequest;
import com.interview.loanengine.payment.event.PaymentMadeEvent;
import com.interview.loanengine.schedule.InstallmentStatus;
import com.interview.loanengine.schedule.Schedule;
import com.interview.loanengine.schedule.ScheduleRepository;
import com.interview.loanengine.transactionlogs.PrepaymentOption;
import com.interview.loanengine.transactionlogs.TransactionLog;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Option C: the lump sum pre-funds whole future installments with no recalculation, and any
 * leftover is stored as a credit against the next payable installment.
 */
@Component
@RequiredArgsConstructor
class AdvanceInstallmentsStrategy implements PrepaymentStrategy {

    private final ScheduleRepository scheduleRepository;
    private final LoanRepository loanRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public PrepaymentOption option() {
        return PrepaymentOption.ADVANCE_INSTALLMENTS;
    }

    @Override
    public PrepaymentRequest apply(Loan loan, List<Schedule> activeSchedule, PrepaymentRequest request, TransactionLog log) {
        int installmentNumber = request.installmentNumber();
        AdvanceInstallmentResult result = ScheduleOps.applyAdvanceInstallments(
                loan, activeSchedule, installmentNumber, request.amount());

        int fullyCovered = result.installmentsFullyCovered();
        int lastCovered = installmentNumber + fullyCovered - 1;

        if (fullyCovered > 0) {
            List<String> coveredIds = activeSchedule.stream()
                    .filter(s -> s.getInstallmentNumber() >= installmentNumber
                            && s.getInstallmentNumber() <= lastCovered)
                    .map(Schedule::getId)
                    .toList();
            eventPublisher.publishEvent(new PaymentMadeEvent(log.getId(), coveredIds));
        }

        BigDecimal leftover = request.amount()
                .subtract(loan.getEquatedMonthlyInstallment().multiply(BigDecimal.valueOf(fullyCovered)));
        if (leftover.signum() > 0) {
            activeSchedule.stream()
                    .filter(s -> s.getInstallmentNumber() == result.nextPayableInstallmentNumber())
                    .findFirst()
                    .ifPresent(next -> {
                        next.setPrepaidAmount(leftover);
                        scheduleRepository.save(next);
                    });
        }

        boolean anyPendingLeft = activeSchedule.stream()
                .anyMatch(s -> s.getStatus() == InstallmentStatus.PENDING
                        && (s.getInstallmentNumber() < installmentNumber || s.getInstallmentNumber() > lastCovered));
        if (!anyPendingLeft) {
            loan.setStatus(LoanStatus.CLOSED);
            loan.setOutstandingBalance(BigDecimal.ZERO);
            loanRepository.save(loan);
        }

        return PrepaymentRequest.advance(log, installmentNumber, request.amount(), result);
    }
}
