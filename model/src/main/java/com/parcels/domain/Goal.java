package com.parcels.domain;

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
@Table(name = "goal", schema = "personal_finance_tracker")
public class Goal extends BaseEntity{

    @Serial
    private static final long serialVersionUID = -9073874759121301956L;

    @Id
    @Column(name = "id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserAccount userAccount;

    @Column(name = "name")
    private String name;

    @Column(name = "target_amount", columnDefinition = "numeric")
    private BigDecimal targetAmount;

    @Column(name = "current_amount", columnDefinition = "numeric")
    private BigDecimal currentAmount;

    @Column(name = "deadline")
    private OffsetDateTime deadline;

    /**
     * Entities are equal when they have the same database id. Two unsaved entities (id == null) are never equal.
     * Uses instanceof and getId() so it also works with Hibernate proxies.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Goal other)) return false;
        return id != null && id.equals(other.getId());
    }

    /**
     * Constant per class so the hash code does not change when the id is assigned on persist.
     */
    @Override
    public int hashCode() {
        return Goal.class.hashCode();
    }
}
