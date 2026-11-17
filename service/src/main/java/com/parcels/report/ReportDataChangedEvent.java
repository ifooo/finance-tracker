package com.parcels.report;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Published when data shown in the reports of {@code months} changed. Listeners run after the commit.
 */
public record ReportDataChangedEvent(Set<YearMonth> months) {

    public ReportDataChangedEvent {
        months = Set.copyOf(months);
    }

    public static ReportDataChangedEvent forDates(Collection<OffsetDateTime> dates, ZoneId zone) {
        return new ReportDataChangedEvent(dates.stream()
                .filter(Objects::nonNull)
                .map(date -> YearMonth.from(date.atZoneSameInstant(zone)))
                .collect(Collectors.toSet()));
    }
}
