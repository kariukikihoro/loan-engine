package com.interview.loanengine.payment;

import com.interview.loanengine.calculations.LoanCalculations;
import com.interview.loanengine.loan.Loan;
import com.interview.loanengine.loan.LoanRepository;
import com.interview.loanengine.loan.LoanStatus;
import com.interview.loanengine.payment.dto.InstallmentPaymentRequest;
import com.interview.loanengine.payment.dto.PrepaymentRequest;
import com.interview.loanengine.payment.event.PaymentMadeEvent;
import com.interview.loanengine.payment.strategy.PrepaymentStrategy;
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
    private final List<PrepaymentStrategy> strategies;

    @Override
    @Transactional
    public InstallmentPaymentRequest payInstallments(String loanId, InstallmentPaymentRequest request) {
        Loan loan = findLoanOrThrow(loanId);

        List<Schedule> pending = scheduleRepository
                .findByLoanIdAndStatusOrderByInstallmentNumberAsc(loanId, InstallmentStatus.PENDING);
        if (pending.isEmpty()) {
            throw new IllegalArgumentException("Loan " + loanId + " has no pending installments to pay");
        }
        if (request.numberOfInstallments() > pending.size()) {
            throw new IllegalArgumentException("Requested " + request.numberOfInstallments()
                    + " installments but only " + pending.size() + " remain pending");
        }

        List<Schedule> toPay = List.copyOf(pending.subList(0, request.numberOfInstallments()));

        BigDecimal amount = toPay.stream()
                .map(s -> s.getPrepaidAmount() == null
                        ? s.getEmiAmount()
                        : s.getEmiAmount().subtract(s.getPrepaidAmount()))
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
            loan.setOutstandingBalance(lastPaid.getPrincipalBalance());
        }
        if (toPay.size() == pending.size()) {
            loan.setStatus(LoanStatus.CLOSED);
            loan.setOutstandingBalance(BigDecimal.ZERO);
        }
        loanRepository.save(loan);

        return InstallmentPaymentRequest.fromTransactionLog(log, toPay, loan.getOutstandingBalance());
    }

    @Override
    @Transactional
    public PrepaymentRequest processPrepayment(String loanId, PrepaymentRequest request) {
        Loan loan = findLoanOrThrow(loanId);
        List<Schedule> activeSchedule = scheduleRepository
                .findByLoanIdAndStatusNotOrderByInstallmentNumberAsc(loanId, InstallmentStatus.ADJUSTED);

        TransactionLog log = transactionLogRepository.save(TransactionLog.builder()
                .transactionDate(LocalDate.now())
                .transactionType(TransactionType.PARTIAL_PREPAYMENT)
                .prepaymentOption(request.option())
                .transactionAmount(LoanCalculations.toMonetaryValue(request.amount()))
                .loan(loan)
                .build());

        return strategyFor(request.option()).apply(loan, activeSchedule, request, log);
    }

    private PrepaymentStrategy strategyFor(PrepaymentOption option) {
        return strategies.stream()
                .filter(strategy -> strategy.option() == option)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported prepayment option: " + option));
    }

    private Loan findLoanOrThrow(String loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanId));
    }
}
