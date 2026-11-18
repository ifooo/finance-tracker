package com.parcels.domain;

import com.parcels.domain.enums.Currency;
import com.parcels.domain.enums.PaymentMethod;
import com.parcels.domain.enums.TransactionSource;
import com.parcels.domain.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.io.Serial;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@Table(name = "transaction", schema = "personal_finance_tracker")
public class Transaction extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1456798519762847263L;

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
    private BigDecimal amount;

    @Column(name = "description")
    private String description;

    @ManyToMany
    @JoinTable(
            schema = "personal_finance_tracker",
            name = "transaction_category",
            joinColumns = @JoinColumn(name = "transaction_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @Builder.Default
    private List<Category> categories = new ArrayList<>();

    @Column(name = "date_from")
    private OffsetDateTime dateFrom;

    @Column(name = "currency")
    @Enumerated(EnumType.STRING)
    private Currency currency;

    @Column(name = "merchant")
    private String merchant;

    @Column(name = "payment_method")
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "notes")
    private String notes;

    @Column(name = "source", nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TransactionSource source = TransactionSource.API;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    @Builder.Default
    private List<TransactionItem> items = new ArrayList<>();

    /** Adds a receipt line and links it back to this transaction. */
    public void addItem(TransactionItem item) {
        item.setTransaction(this);
        items.add(item);
    }

    /**
     * Entities are equal when they have the same database id. Two unsaved entities (id == null) are never equal.
     * Uses instanceof and getId() so it also works with Hibernate proxies.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Transaction other)) return false;
        return id != null && id.equals(other.getId());
    }

    /**
     * Constant per class so the hash code does not change when the id is assigned on persist.
     */
    @Override
    public int hashCode() {
        return Transaction.class.hashCode();
    }
}
