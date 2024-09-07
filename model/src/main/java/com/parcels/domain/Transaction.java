package com.parcels.domain;

import com.parcels.domain.enums.Currency;
import com.parcels.domain.enums.TransactionType;
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
@Table(name = "transaction", schema = "personal_finance_tracker")
public class Transaction extends BaseEntity {

    @Serial
    private static final long serialVersionUID = -3457001373122395190L;

    @Id
    @Column(name = "id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserAccount userAccount;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Column(name = "amount", columnDefinition = "numeric")
    private Double amount;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "category_id", referencedColumnName = "id")
    private Category category;

    @Column(name = "date_from")
    private OffsetDateTime dateFrom;

    @Column(name = "currency")
    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        Transaction that = (Transaction) o;

        return new EqualsBuilder().appendSuper(super.equals(o)).append(id, that.id).append(userAccount, that.userAccount).append(transactionType, that.transactionType).append(amount, that.amount).append(description, that.description).append(category, that.category).append(dateFrom, that.dateFrom).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(id).append(userAccount).append(transactionType).append(amount).append(description).append(category).append(dateFrom).toHashCode();
    }
}
