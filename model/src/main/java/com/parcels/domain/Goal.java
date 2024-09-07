package com.parcels.domain;

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
    private Double targetAmount;

    @Column(name = "current_amount", columnDefinition = "numeric")
    private Double currentAmount;

    @Column(name = "deadline")
    private OffsetDateTime deadline;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Goal goal = (Goal) o;

        return new EqualsBuilder().appendSuper(super.equals(o)).append(id, goal.id).append(userAccount, goal.userAccount).append(name, goal.name).append(targetAmount, goal.targetAmount).append(currentAmount, goal.currentAmount).append(deadline, goal.deadline).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(id).append(userAccount).append(name).append(targetAmount).append(currentAmount).append(deadline).toHashCode();
    }
}
