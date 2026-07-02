package com.interview.loanengine.utilities.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized, null-safe {@link Specification} building blocks shared by every searchable entity.
 * Each helper returns an always-true predicate when its filter value is absent, so callers can
 * freely {@code and()} them together and let missing (optional) parameters simply not filter.
 */
public final class SearchSpecifications {

    private SearchSpecifications() {
    }

    public static <T> Specification<T> like(String field, String value) {
        return (root, query, cb) -> (value == null || value.isBlank())
                ? cb.conjunction()
                : cb.like(cb.lower(root.get(field)), "%" + value.trim().toLowerCase() + "%");
    }


    public static <T> Specification<T> equal(String field, Object value) {
        return (root, query, cb) -> value == null ? cb.conjunction() : cb.equal(root.get(field), value);
    }

    public static <T, Y extends Comparable<? super Y>> Specification<T> range(String field, Y from, Y to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get(field), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get(field), to));
            }
            return predicates.isEmpty() ? cb.conjunction() : cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
