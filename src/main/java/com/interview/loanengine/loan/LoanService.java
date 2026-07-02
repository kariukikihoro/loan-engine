package com.interview.loanengine.loan;

import com.interview.loanengine.schedule.ScheduleResponse;

import java.math.BigDecimal;
import java.util.List;

public interface LoanService {

    LoanRequest createLoan(LoanRequest request);

    LoanRequest findLoanById(String loanId);

    List<ScheduleResponse> findLoanSchedules(String loanId);

    List<LoanRequest> searchLoans(Integer tenure, BigDecimal loanedAmountFrom, BigDecimal loanedAmountTo);
}
