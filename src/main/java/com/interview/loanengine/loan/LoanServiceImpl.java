package com.interview.loanengine.loan;

import com.interview.loanengine.calculations.LoanCalculations;
import com.interview.loanengine.calculations.ScheduleOps;
import com.interview.loanengine.loanproduct.LoanProduct;
import com.interview.loanengine.loanproduct.LoanProductRepository;
import com.interview.loanengine.schedule.InstallmentStatus;
import com.interview.loanengine.schedule.Schedule;
import com.interview.loanengine.schedule.ScheduleRepository;
import com.interview.loanengine.schedule.ScheduleResponse;
import com.interview.loanengine.utilities.exceptions.LoanNotFoundException;
import com.interview.loanengine.utilities.exceptions.ResourceNotFoundException;
import com.interview.loanengine.utilities.specification.SearchSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final LoanProductRepository loanProductRepository;
    private final ScheduleRepository scheduleRepository;

    @Override
    @Transactional
    public LoanRequest createLoan(LoanRequest request) {

        LoanProduct product = loanProductRepository.findById(request.loanProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan product not found: " + request.loanProductId()));

        Loan loan = loanRepository.save(LoanRequest.toEntity(request, product));

        List<Schedule> schedule = ScheduleOps.generateSchedule(loan, request.firstPaymentDate());
        schedule.forEach(installment -> installment.setStatus(InstallmentStatus.PENDING));
        scheduleRepository.saveAll(schedule);

        return LoanRequest.fromLoan(loan, schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanRequest findLoanById(String loanId) {
        Loan loan = findOrThrow(loanId);
        List<Schedule> schedule = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
        return LoanRequest.fromLoan(loan, schedule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleResponse> findLoanSchedules(String loanId) {
        findOrThrow(loanId);
        return scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId).stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoanRequest> searchLoans(Integer tenure, BigDecimal loanedAmountFrom, BigDecimal loanedAmountTo) {
        Specification<Loan> specification = SearchSpecifications.<Loan>equal("tenure", tenure)
                .and(SearchSpecifications.range("loanedAmount", loanedAmountFrom, loanedAmountTo));
        return loanRepository.findAll(specification).stream()
                .map(loan -> LoanRequest.fromLoan(loan, List.of()))
                .toList();
    }

    private Loan findOrThrow(String loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new LoanNotFoundException("Loan not found: " + loanId));
    }
}
