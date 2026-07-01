package com.interview.loanengine.loan;

import com.interview.loanengine.utilities.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Entity
@Table
public class Loan extends BaseEntity {

    private BigDecimal loanedAmount;

    private BigDecimal outstandingBalance;

    private BigDecimal interestRate;

    private Integer tenure;
}
