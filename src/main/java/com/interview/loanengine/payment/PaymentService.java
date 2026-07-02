package com.interview.loanengine.payment;

import com.interview.loanengine.payment.dto.InstallmentPaymentRequest;
import com.interview.loanengine.payment.dto.PrepaymentRequest;

public interface PaymentService {

    InstallmentPaymentRequest payInstallments(String loanId, InstallmentPaymentRequest request);

    PrepaymentRequest processPrepayment(String loanId, PrepaymentRequest request);
}
