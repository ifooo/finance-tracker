package com.parcels.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.time.OffsetDateTime;

/**
 * A file that was successfully imported from the inbox folder. The SHA-256 of its content is unique,
 * so dropping the same file again does not create duplicate transactions.
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@Table(name = "import_file", schema = "personal_finance_tracker")
public class ImportFile extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 5128829360418475211L;

    @Id
    @Column(name = "id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "sha256", nullable = false, unique = true)
    private String sha256;

    @Column(name = "transaction_count", nullable = false)
    private int transactionCount;

    @Column(name = "imported_at", nullable = false)
    private OffsetDateTime importedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ImportFile other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return ImportFile.class.hashCode();
    }
}
