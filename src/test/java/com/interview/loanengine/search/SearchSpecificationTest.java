package com.interview.loanengine.search;

import com.interview.loanengine.loan.Loan;
import com.interview.loanengine.loan.LoanRepository;
import com.interview.loanengine.loan.LoanRequest;
import com.interview.loanengine.loan.LoanServiceImpl;
import com.interview.loanengine.loanproduct.LoanProduct;
import com.interview.loanengine.loanproduct.LoanProductRepository;
import com.interview.loanengine.loanproduct.LoanProductRequest;
import com.interview.loanengine.loanproduct.LoanProductServiceImpl;
import com.interview.loanengine.schedule.InstallmentStatus;
import com.interview.loanengine.schedule.Schedule;
import com.interview.loanengine.schedule.ScheduleRepository;
import com.interview.loanengine.schedule.ScheduleResponse;
import com.interview.loanengine.schedule.ScheduleServiceImpl;
import com.interview.loanengine.utilities.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({LoanProductServiceImpl.class, LoanServiceImpl.class, ScheduleServiceImpl.class})
class SearchSpecificationTest {

    @Autowired
    private LoanProductRepository loanProductRepository;
    @Autowired
    private LoanRepository loanRepository;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private LoanProductServiceImpl loanProductService;
    @Autowired
    private LoanServiceImpl loanService;
    @Autowired
    private ScheduleServiceImpl scheduleService;

    private static final Pageable FIRST_PAGE = PageRequest.of(0, 10);

    private LoanProduct homeProduct;
    private LoanProduct autoProduct;
    private Loan homeLoan;
    private Loan autoLoan;

    @BeforeEach
    void seed() {
        homeProduct = loanProductRepository.save(LoanProduct.builder()
                .productName("Home Loan").productDescription("home").tenureInMonths(60)
                .interestRate(new BigDecimal("12")).firstPaymentMonth(7).build());
        autoProduct = loanProductRepository.save(LoanProduct.builder()
                .productName("Auto Loan").productDescription("auto").tenureInMonths(36)
                .interestRate(new BigDecimal("18")).firstPaymentMonth(1).build());

        homeLoan = loanRepository.save(Loan.builder()
                .loanedAmount(new BigDecimal("1000000")).outstandingBalance(new BigDecimal("1000000"))
                .equatedMonthlyInstallment(new BigDecimal("22244.45")).tenure(60).loanProduct(homeProduct).build());
        autoLoan = loanRepository.save(Loan.builder()
                .loanedAmount(new BigDecimal("500000")).outstandingBalance(new BigDecimal("500000"))
                .equatedMonthlyInstallment(new BigDecimal("18076.63")).tenure(36).loanProduct(autoProduct).build());

        scheduleRepository.save(schedule(homeLoan, 1));
        scheduleRepository.save(schedule(homeLoan, 2));
        scheduleRepository.save(schedule(autoLoan, 1));
    }

    private Schedule schedule(Loan loan, int number) {
        return Schedule.builder()
                .installmentNumber(number)
                .scheduledDate(LocalDate.of(2024, 7, 24).plusMonths(number - 1))
                .principalAmount(new BigDecimal("100"))
                .interest(new BigDecimal("10"))
                .emiAmount(new BigDecimal("110"))
                .runningBalance(new BigDecimal("110"))
                .principalRunningBalance(BigDecimal.ZERO)
                .status(InstallmentStatus.PENDING)
                .loan(loan)
                .build();
    }

    @Test
    void productSearchByNameUsesLike() {
        PageResponse<LoanProductRequest> results =
                loanProductService.searchLoanProducts("home", null, null, null, null, FIRST_PAGE);
        assertEquals(1, results.totalElements());
        assertEquals("Home Loan", results.content().get(0).productName());
    }

    @Test
    void productSearchByInterestAndTenureRanges() {
        assertEquals(List.of("Auto Loan"),
                loanProductService.searchLoanProducts(null, new BigDecimal("15"), null, null, null, FIRST_PAGE)
                        .content().stream().map(LoanProductRequest::productName).toList());
        assertEquals(List.of("Home Loan"),
                loanProductService.searchLoanProducts(null, null, null, 40, 70, FIRST_PAGE)
                        .content().stream().map(LoanProductRequest::productName).toList());
    }

    @Test
    void productSearchWithNoFiltersReturnsAll() {
        assertEquals(2, loanProductService.searchLoanProducts(null, null, null, null, null, FIRST_PAGE)
                .content().size());
    }

    @Test
    void productSearchHonoursPageSizeAndReportsPageInfo() {
        PageResponse<LoanProductRequest> page =
                loanProductService.searchLoanProducts(null, null, null, null, null, PageRequest.of(0, 1));
        assertEquals(1, page.content().size());
        assertEquals(2, page.totalElements());
        assertEquals(2, page.totalPages());
        assertEquals(0, page.number());
        assertEquals(1, page.size());
    }

    @Test
    void loanSearchByTenureAndLoanedAmountRange() {
        assertEquals(1, loanService.searchLoans(60, null, null, FIRST_PAGE).getTotalElements());
        // loanedAmount from 600000 -> only the 1,000,000 home loan
        var range = loanService.searchLoans(null, new BigDecimal("600000"), null, FIRST_PAGE);
        assertEquals(1, range.getTotalElements());
        assertEquals(homeLoan.getId(), range.getContent().get(0).id());
    }

    @Test
    void scheduleSearchUsesJpqlByLoanAndProduct() {
        assertEquals(2, scheduleService.searchSchedules(homeLoan.getId(), null, FIRST_PAGE).totalElements());
        assertEquals(1, scheduleService.searchSchedules(null, autoProduct.getId(), FIRST_PAGE).totalElements());

        PageResponse<ScheduleResponse> all = scheduleService.searchSchedules(null, null, FIRST_PAGE);
        assertEquals(3, all.totalElements());
        assertTrue(all.content().stream().anyMatch(s -> s.installmentNumber() == 2));
    }
}
