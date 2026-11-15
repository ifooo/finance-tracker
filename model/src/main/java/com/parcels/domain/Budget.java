package com.parcels.domain;

import com.parcels.domain.enums.Period;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.io.Serial;
import java.time.OffsetDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@Table(name = "budget", schema = "personal_finance_tracker")
public class Budget extends BaseEntity {

    @Serial
    private static final long serialVersionUID = -936195977270086140L;

    @Id
    @Column(name = "id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserAccount user;

    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private Category category;

    @Column(name = "amount", columnDefinition = "numeric")
    private BigDecimal amount;

    @Column(name = "budget_period")
    @Enumerated(EnumType.STRING)
    private Period period;

    @Column(name = "start_date")
    private OffsetDateTime startDate;

    @Column(name = "end_date")
    private OffsetDateTime endDate;

    /**
     * Entities are equal when they have the same database id. Two unsaved entities (id == null) are never equal.
     * Uses instanceof and getId() so it also works with Hibernate proxies.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Budget other)) return false;
        return id != null && id.equals(other.getId());
    }

    /**
     * Constant per class so the hash code does not change when the id is assigned on persist.
     */
    @Override
    public int hashCode() {
        return Budget.class.hashCode();
    }
}
