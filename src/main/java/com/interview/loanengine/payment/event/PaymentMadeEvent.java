package com.interview.loanengine.payment.event;

import java.util.List;

/**
 * Published when a payment settles one or more installments. Consumed asynchronously of the
 * request logic to mark those installments PAID and attach them to the created transaction log.
 *
 * @param transactionLogId the persisted transaction log recording the payment
 * @param paidScheduleIds  ids of the schedule installments settled by this payment
 */
public record PaymentMadeEvent(String transactionLogId, List<String> paidScheduleIds) {
}
