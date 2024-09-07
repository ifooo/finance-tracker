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
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        UserAccount that = (UserAccount) o;

        return new EqualsBuilder().append(id, that.id).append(fullName, that.fullName).append(email, that.email).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(id).append(fullName).append(email).toHashCode();
    }
}
