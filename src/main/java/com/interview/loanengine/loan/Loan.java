package com.interview.loanengine.loan;

import com.interview.loanengine.loanproduct.LoanProduct;
import com.interview.loanengine.utilities.BaseEntity;
import jakarta.persistence.Entity;
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

    private BigDecimal loanedAmount;

    private BigDecimal principalAmount;

    private BigDecimal interestAmount;

    private BigDecimal outstandingBalance;

    private BigDecimal equatedMonthlyInstallment;

    private Integer tenure;

    @ManyToOne
    private LoanProduct loanProduct;
}
