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
 * Records that the monthly PDF report for {@code month} (yyyy-MM) was generated.
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Getter
@Setter
@Table(name = "report_run", schema = "personal_finance_tracker")
public class ReportRun extends BaseEntity {

    @Serial
    private static final long serialVersionUID = -4471395713285573380L;

    @Id
    @Column(name = "id", columnDefinition = "serial")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "month", nullable = false, unique = true, length = 7)
    private String month;

    @Column(name = "generated_at", nullable = false)
    private OffsetDateTime generatedAt;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReportRun other)) return false;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return ReportRun.class.hashCode();
    }
}
