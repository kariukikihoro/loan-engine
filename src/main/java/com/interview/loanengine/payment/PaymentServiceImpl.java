package com.interview.loanengine.payment;

import com.interview.loanengine.calculations.AdvanceInstallmentResult;
import com.interview.loanengine.calculations.LoanCalculations;
import com.interview.loanengine.calculations.RescheduleResult;
import com.interview.loanengine.calculations.ScheduleOps;
import com.interview.loanengine.loan.Loan;
import com.interview.loanengine.loan.LoanRepository;
import com.interview.loanengine.payment.dto.InstallmentPaymentRequest;
import com.interview.loanengine.payment.dto.PrepaymentRequest;
import com.interview.loanengine.payment.event.PaymentMadeEvent;
import com.interview.loanengine.schedule.InstallmentStatus;
import com.interview.loanengine.schedule.Schedule;
import com.interview.loanengine.schedule.ScheduleRepository;
import com.interview.loanengine.transactionlogs.PrepaymentOption;
import com.interview.loanengine.transactionlogs.TransactionLog;
import com.interview.loanengine.transactionlogs.TransactionLogRepository;
import com.interview.loanengine.transactionlogs.TransactionType;
import com.interview.loanengine.utilities.exceptions.LoanNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final LoanRepository loanRepository;
    private final ScheduleRepository scheduleRepository;
    private final TransactionLogRepository transactionLogRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public InstallmentPaymentRequest payInstallments(String loanId, InstallmentPaymentRequest request) {
        Loan loan = findLoanOrThrow(loanId);

        List<Schedule> pending = scheduleRepository
                .findByLoanIdAndStatusOrderByInstallmentNumberAsc(loanId, InstallmentStatus.PENDING);
        if (pending.isEmpty()) {
            throw new IllegalArgumentException("Loan " + loanId + " has no pending installments to pay");
        }

        int count = Math.min(request.numberOfInstallments(), pending.size());
        List<Schedule> toPay = List.copyOf(pending.subList(0, count));

        BigDecimal amount = toPay.stream()
                .map(Schedule::getEmiAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        TransactionLog log = transactionLogRepository.save(TransactionLog.builder()
                .transactionDate(LocalDate.now())
                .transactionType(TransactionType.EMI_PAYMENT)
                .transactionAmount(LoanCalculations.toMonetaryValue(amount))
                .loan(loan)
                .build());

        eventPublisher.publishEvent(new PaymentMadeEvent(log.getId(),
                toPay.stream().map(Schedule::getId).toList()));

        Schedule lastPaid = toPay.get(toPay.size() - 1);

        if (lastPaid.getPrincipalBalance() != null) {
            loan.setOutstandingBalance(LoanCalculations.toMonetaryValue(lastPaid.getPrincipalBalance()));
            loanRepository.save(loan);
        }

        return InstallmentPaymentRequest.fromTransactionLog(log, toPay, loan.getOutstandingBalance());
    }

    @Override
    @Transactional
    public PrepaymentRequest processPrepayment(String loanId, PrepaymentRequest request) {
        Loan loan = findLoanOrThrow(loanId);
        List<Schedule> baseSchedule = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);

        PrepaymentOption option = request.option();
        int installmentNumber = request.installmentNumber();
        BigDecimal amount = request.amount();

        TransactionLog log = transactionLogRepository.save(TransactionLog.builder()
                .transactionDate(LocalDate.now())
                .transactionType(TransactionType.PARTIAL_PREPAYMENT)
                .prepaymentOption(option)
                .transactionAmount(LoanCalculations.toMonetaryValue(amount))
                .loan(loan)
                .build());

        return switch (option) {
            case REDUCE_EMI_KEEP_TENOR -> {
                RescheduleResult result = ScheduleOps.applyReduceEmiKeepTenor(
                        loan, baseSchedule, installmentNumber, amount);
                reschedule(loan, result, installmentNumber, log);
                loan.setEquatedMonthlyInstallment(result.newEmi());
                loan.setOutstandingBalance(LoanCalculations.toMonetaryValue(result.newPrincipal()));
                loanRepository.save(loan);
                yield PrepaymentRequest.reschedule(log, option, amount, result);
            }
            case REDUCE_TENOR_KEEP_EMI -> {
                RescheduleResult result = ScheduleOps.applyReduceTenorKeepEmi(
                        loan, baseSchedule, installmentNumber, amount);
                reschedule(loan, result, installmentNumber, log);
                loan.setTenure((installmentNumber - 1) + result.newTenor());
                loan.setOutstandingBalance(LoanCalculations.toMonetaryValue(result.newPrincipal()));
                loanRepository.save(loan);
                yield PrepaymentRequest.reschedule(log, option, amount, result);
            }
            case ADVANCE_INSTALLMENTS -> {
                AdvanceInstallmentResult result = ScheduleOps.applyAdvanceInstallments(
                        loan, baseSchedule, installmentNumber, amount);
                coverInAdvance(loan, installmentNumber, result.installmentsFullyCovered(), log);
                yield PrepaymentRequest.advance(log, installmentNumber, amount, result);
            }
        };
    }

    /**
     * Options A/B: supersede the remaining installments (mark ADJUSTED, link the log) and persist
     * the freshly recalculated schedule as PENDING.
     */
    private void reschedule(Loan loan, RescheduleResult result, int installmentNumber, TransactionLog log) {
        List<Schedule> superseded = scheduleRepository
                .findByLoanIdAndInstallmentNumberGreaterThanEqualOrderByInstallmentNumberAsc(loan.getId(), installmentNumber);
        for (Schedule schedule : superseded) {
            schedule.setStatus(InstallmentStatus.ADJUSTED);
            schedule.setTransactionLog(log);
        }
        scheduleRepository.saveAll(superseded);

        List<Schedule> recalculated = result.schedule();
        recalculated.forEach(schedule -> {
            schedule.setStatus(InstallmentStatus.PENDING);
            schedule.setTransactionLog(log);
        });
        scheduleRepository.saveAll(recalculated);
    }

    /**
     * Option C: the lump sum pre-funds whole future installments. Those covered installments are
     * settled in advance via the payment event (marked PAID and linked to the log).
     */
    private void coverInAdvance(Loan loan, int installmentNumber, int fullyCovered, TransactionLog log) {
        if (fullyCovered <= 0) {
            return;
        }
        int lastCovered = installmentNumber + fullyCovered - 1;

        List<Schedule> covered = scheduleRepository
                .findByLoanIdAndInstallmentNumberBetweenOrderByInstallmentNumberAsc(loan.getId(), installmentNumber, lastCovered);
        eventPublisher.publishEvent(new PaymentMadeEvent(log.getId(),
                covered.stream().map(Schedule::getId).toList()));
    }

    private Loan findLoanOrThrow(String loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanId));
    }
}
