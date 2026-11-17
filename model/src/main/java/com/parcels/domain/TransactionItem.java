package com.parcels.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A single receipt line belonging to a {@link Transaction}.
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Table(name = "transaction_item", schema = "personal_finance_tracker")
public class TransactionItem implements Serializable {

    @Serial
    private static final long serialVersionUID = -2731998143402950617L;

    @Id
    @Column(name = "id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "quantity")
    private BigDecimal quantity;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "total", nullable = false)
    private BigDecimal total;

    /**
     * Entities are equal when they have the same database id. Two unsaved entities (id == null) are never equal.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransactionItem other)) return false;
        return id != null && id.equals(other.getId());
    }

    /**
     * Constant per class so the hash code does not change when the id is assigned on persist.
     */
    @Override
    public int hashCode() {
        return TransactionItem.class.hashCode();
    }
}
