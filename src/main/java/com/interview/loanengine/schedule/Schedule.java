package com.interview.loanengine.schedule;

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
public class Schedule extends BaseEntity {
}
