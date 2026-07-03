package com.interview.loanengine.transactionlogs;

import com.interview.loanengine.loan.Loan;
import com.interview.loanengine.utilities.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table
public class TransactionLog extends BaseEntity {

    private LocalDate transactionDate;

    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    private PrepaymentOption prepaymentOption;

    @Column(precision = 23, scale = 10)
    private BigDecimal transactionAmount;

    @ManyToOne
    private Loan loan;
}
