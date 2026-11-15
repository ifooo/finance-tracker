package com.parcels.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@Table(name = "user_account", schema = "personal_finance_tracker")
public class UserAccount implements Serializable {

    @Serial
    private static final long serialVersionUID = 7628497573733216875L;

    @Id
    @Column(name = "id", columnDefinition = "serial")
    private Long id;

    @Column(name = "name")
    private String fullName;

    @Column(name = "email")
    @Email
    private String email;

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
