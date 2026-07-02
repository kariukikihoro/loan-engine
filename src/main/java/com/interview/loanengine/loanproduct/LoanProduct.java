package com.interview.loanengine.loanproduct;

import com.interview.loanengine.utilities.BaseEntity;
import jakarta.persistence.Column;
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
public class LoanProduct extends BaseEntity {

    @Column(unique = true, nullable = false)
    private String productName;

    @Column(nullable = false)
    private String productDescription;

    @Column(nullable = false)
    private Integer tenureInMonths;

    @Column(nullable = false)
    private BigDecimal interestRate;

    @Column(nullable = false)
    private Integer firstPaymentMonth;
}
