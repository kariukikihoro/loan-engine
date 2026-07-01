package com.interview.loanengine.loan;

import com.interview.loanengine.utilities.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
@Entity
@Table
public class Loan extends BaseEntity {
}
