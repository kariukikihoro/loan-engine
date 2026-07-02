package com.interview.loanengine.payment.event;

import com.interview.loanengine.schedule.InstallmentStatus;
import com.interview.loanengine.schedule.Schedule;
import com.interview.loanengine.schedule.ScheduleRepository;
import com.interview.loanengine.transactionlogs.TransactionLog;
import com.interview.loanengine.transactionlogs.TransactionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Consumes {@link PaymentMadeEvent}: marks the fromTransactionLog installments as PAID and attaches them to the
 * transaction log that recorded the payment. Runs within the publishing transaction so the status
 * change and the log linkage are committed atomically with the payment.
 */
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final ScheduleRepository scheduleRepository;
    private final TransactionLogRepository transactionLogRepository;

    @EventListener
    public void onPaymentMade(PaymentMadeEvent event) {
        TransactionLog log = transactionLogRepository.findById(event.transactionLogId())
                .orElseThrow(() -> new IllegalStateException(
                        "Transaction log not found: " + event.transactionLogId()));

        List<Schedule> paidSchedules = scheduleRepository.findAllById(event.paidScheduleIds());
        for (Schedule schedule : paidSchedules) {
            schedule.setStatus(InstallmentStatus.PAID);
            schedule.setTransactionLog(log);
        }
        scheduleRepository.saveAll(paidSchedules);
    }
}
