package com.parcels.domain;

import com.parcels.domain.enums.Period;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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
    private Double amount;

    @Column(name = "budget_period")
    @Enumerated(EnumType.STRING)
    private Period period;

    @Column(name = "start_date")
    private OffsetDateTime startDate;

    @Column(name = "end_date")
    private OffsetDateTime endDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Budget budget = (Budget) o;

        return new EqualsBuilder().appendSuper(super.equals(o)).append(id, budget.id).append(user, budget.user).append(category, budget.category).append(amount, budget.amount).append(startDate, budget.startDate).append(endDate, budget.endDate).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(id).append(user).append(category).append(amount).append(startDate).append(endDate).toHashCode();
    }
}
