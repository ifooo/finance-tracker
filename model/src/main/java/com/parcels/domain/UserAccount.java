package com.parcels.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

/**
 * A person in the household whose transactions are tracked.
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@Table(name = "user_account", schema = "personal_finance_tracker")
public class UserAccount extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 7628497573733216875L;

    @Id
    @Column(name = "id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short, unique id, e.g. "ivo"; also accepted as the "person" of a transaction. */
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "name")
    private String fullName;

    @Column(name = "email")
    @Email
    private String email;

    /** Name to show in reports: the full name, or the username when there is none. */
    public String displayName() {
        return fullName == null || fullName.isBlank() ? username : fullName;
    }

    /**
     * Entities are equal when they have the same database id. Two unsaved entities (id == null) are never equal.
     * Uses instanceof and getId() so it also works with Hibernate proxies.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserAccount other)) return false;
        return id != null && id.equals(other.getId());
    }

    /**
     * Constant per class so the hash code does not change when the id is assigned on persist.
     */
    @Override
    public int hashCode() {
        return UserAccount.class.hashCode();
    }
}
