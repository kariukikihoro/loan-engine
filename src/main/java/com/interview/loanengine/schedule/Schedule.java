package com.interview.loanengine.schedule;

import com.interview.loanengine.loan.Loan;
import com.interview.loanengine.utilities.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.web.bind.annotation.PostMapping;

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

    private BigDecimal principalAmount;

    private BigDecimal interest;

    private BigDecimal emiAmount;

    private BigDecimal runningBalance;

    private BigDecimal principalRunningBalance;

    @Transient
    private BigDecimal principalBalance;

    @ManyToOne
    private Loan loan;

    @PostLoad
    protected void postLoad() {

        principalBalance = loan.getLoanedAmount().subtract(principalRunningBalance);
    }
}
