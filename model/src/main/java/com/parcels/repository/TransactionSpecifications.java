package com.parcels.repository;

import com.parcels.domain.Transaction;
import com.parcels.domain.enums.TransactionType;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;

/**
 * Query building blocks for {@link Transaction}, combined with {@link Specification#and}.
 */
public final class TransactionSpecifications {

    private TransactionSpecifications() {
    }

    public static Specification<Transaction> hasType(TransactionType type) {
        return (root, query, cb) -> cb.equal(root.get("transactionType"), type);
    }

    /** Inclusive lower bound on {@code dateFrom}. */
    public static Specification<Transaction> dateOnOrAfter(OffsetDateTime from) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("dateFrom"), from);
    }

    /** Inclusive upper bound on {@code dateFrom}. */
    public static Specification<Transaction> dateOnOrBefore(OffsetDateTime to) {
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("dateFrom"), to);
    }
}
