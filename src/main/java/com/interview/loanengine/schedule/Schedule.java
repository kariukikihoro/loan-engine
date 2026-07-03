package com.interview.loanengine.schedule;

import com.interview.loanengine.loan.Loan;
import com.interview.loanengine.transactionlogs.TransactionLog;
import com.interview.loanengine.utilities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Entity
@Table
public class Schedule extends BaseEntity {

    private Integer installmentNumber;

    private LocalDate scheduledDate;

    @Column(precision = 23, scale = 10)
    private BigDecimal principalAmount;

    @Column(precision = 23, scale = 10)
    private BigDecimal interest;

    @Column(precision = 23, scale = 10)
    private BigDecimal emiAmount;

    @Column(precision = 23, scale = 10)
    private BigDecimal runningBalance;

    @Column(precision = 23, scale = 10)
    private BigDecimal principalRunningBalance;

    @Column(precision = 23, scale = 10)
    private BigDecimal principalBalance;

    @Column(precision = 23, scale = 10)
    private BigDecimal prepaidAmount;

    @Enumerated(EnumType.STRING)
    private InstallmentStatus status;

    @ManyToOne
    private Loan loan;

    @ManyToOne
    private TransactionLog transactionLog;
}
