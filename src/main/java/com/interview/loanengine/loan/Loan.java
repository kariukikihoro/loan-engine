package com.interview.loanengine.loan;

import com.interview.loanengine.loanproduct.LoanProduct;
import com.interview.loanengine.utilities.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table
public class Loan extends BaseEntity {

    @Column(precision = 23, scale = 10)
    private BigDecimal loanedAmount;

    @Column(precision = 23, scale = 10)
    private BigDecimal principalAmount;

    @Column(precision = 23, scale = 10)
    private BigDecimal interestAmount;

    @Column(precision = 23, scale = 10)
    private BigDecimal outstandingBalance;

    @Column(precision = 23, scale = 10)
    private BigDecimal equatedMonthlyInstallment;

    private Integer tenure;

    @Enumerated(EnumType.STRING)
    private LoanStatus status;

    @ManyToOne
    private LoanProduct loanProduct;
}
