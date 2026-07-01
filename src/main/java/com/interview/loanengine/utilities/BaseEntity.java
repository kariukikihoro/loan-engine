package com.interview.loanengine.utilities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
@Getter
@Setter
@ToString
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private Boolean deleted;

    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        deleted = false;
    }
}
