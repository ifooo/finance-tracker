package com.parcels.domain.enums;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PeriodTest {

    private static OffsetDateTime date(int year, int month, int day) {
        return OffsetDateTime.of(year, month, day, 0, 0, 0, 0, ZoneOffset.UTC);
    }

    @Test
    void weeklyEndsSevenDaysLater() {
        assertEquals(date(2026, 11, 8), Period.WEEKLY.endDateFrom(date(2026, 11, 1)));
    }

    @Test
    void monthlyEndsOneCalendarMonthLater() {
        assertEquals(date(2026, 12, 1), Period.MONTHLY.endDateFrom(date(2026, 11, 1)));
    }

    @Test
    void monthlyFromMonthEndClampsToShorterMonth() {
        assertEquals(date(2026, 2, 28), Period.MONTHLY.endDateFrom(date(2026, 1, 31)));
        assertEquals(date(2028, 2, 29), Period.MONTHLY.endDateFrom(date(2028, 1, 31)));
    }

    @Test
    void yearlyEndsOneCalendarYearLater() {
        assertEquals(date(2027, 11, 1), Period.YEARLY.endDateFrom(date(2026, 11, 1)));
    }

    @Test
    void preservesTimeAndOffset() {
        OffsetDateTime start = OffsetDateTime.of(2026, 11, 1, 9, 30, 0, 0, ZoneOffset.ofHours(1));
        assertEquals(OffsetDateTime.of(2026, 12, 1, 9, 30, 0, 0, ZoneOffset.ofHours(1)), Period.MONTHLY.endDateFrom(start));
    }
}
