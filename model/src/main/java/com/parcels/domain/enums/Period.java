package com.parcels.domain.enums;

import java.time.OffsetDateTime;

public enum Period {
    WEEKLY,
    MONTHLY,
    YEARLY;

    /**
     * End of a budget period that begins at {@code start} (exclusive: the next period starts here).
     * Month and year arithmetic is calendar-aware, e.g. Jan 31 + 1 month = Feb 28/29.
     */
    public OffsetDateTime endDateFrom(OffsetDateTime start) {
        return switch (this) {
            case WEEKLY -> start.plusWeeks(1);
            case MONTHLY -> start.plusMonths(1);
            case YEARLY -> start.plusYears(1);
        };
    }
}
