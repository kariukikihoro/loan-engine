package com.interview.loanengine.schedule;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, String> {

    /**
     * Paginated JPQL search by loan and/or loan product; either filter is optional (null = not applied).
     */
    @Query(value = """
            select s from Schedule s
            where (:loanId is null or s.loan.id = :loanId)
              and (:loanProductId is null or s.loan.loanProduct.id = :loanProductId)
            order by s.installmentNumber asc
            """,
            countQuery = """
                    select count(s) from Schedule s
                    where (:loanId is null or s.loan.id = :loanId)
                      and (:loanProductId is null or s.loan.loanProduct.id = :loanProductId)
                    """)
    Page<Schedule> searchByLoanAndProduct(@Param("loanId") String loanId,
                                          @Param("loanProductId") String loanProductId,
                                          Pageable pageable);

    List<Schedule> findByLoanIdOrderByInstallmentNumberAsc(String loanId);

    List<Schedule> findByLoanIdAndStatusOrderByInstallmentNumberAsc(String loanId, InstallmentStatus status);

    /** The "active" schedule: every installment that has not been superseded by a reschedule. */
    List<Schedule> findByLoanIdAndStatusNotOrderByInstallmentNumberAsc(String loanId, InstallmentStatus status);

    List<Schedule> findByLoanIdAndInstallmentNumberGreaterThanEqualOrderByInstallmentNumberAsc(
            String loanId, Integer installmentNumber);

    List<Schedule> findByLoanIdAndInstallmentNumberBetweenOrderByInstallmentNumberAsc(
            String loanId, Integer start, Integer end);
}
