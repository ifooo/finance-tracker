package com.parcels.report;

import com.parcels.domain.enums.Currency;
import com.parcels.domain.enums.PaymentMethod;
import com.parcels.domain.enums.TransactionSource;
import com.parcels.domain.enums.TransactionType;
import com.parcels.report.dto.CategoryTotal;
import com.parcels.report.dto.CurrencySummary;
import com.parcels.report.dto.MerchantTotal;
import com.parcels.report.dto.MonthlyReport;
import com.parcels.report.dto.PersonSummary;
import com.parcels.transaction.dto.TransactionDto;
import com.parcels.transaction.dto.TransactionItemDto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hand-built reports for rendering and export tests that need no database. Uses a Cyrillic merchant name on purpose.
 */
public final class ReportFixtures {

    static final String IVO = "Ivo Test";
    static final String ANA = "Ana Test";

    private ReportFixtures() {
    }

    /** Two people, two currencies. */
    public static MonthlyReport sampleReport() {
        OffsetDateTime date = OffsetDateTime.of(2026, 10, 5, 18, 0, 0, 0, ZoneOffset.UTC);
        TransactionDto groceries = new TransactionDto(1L, TransactionType.EXPENSE, new BigDecimal("1250.00"), Currency.MKD,
                "Weekly <groceries>", List.of(6L), List.of("Groceries"), date, "Веро Центар", PaymentMethod.CARD, null,
                TransactionSource.INBOX_JSON,
                List.of(new TransactionItemDto("Milk", new BigDecimal("2"), new BigDecimal("65.00"), new BigDecimal("130.00")),
                        new TransactionItemDto("Bread", null, null, new BigDecimal("45.00"))),
                "ivo", IVO);
        TransactionDto salary = new TransactionDto(2L, TransactionType.INCOME, new BigDecimal("60000.00"), Currency.MKD,
                null, List.of(1L), List.of("Salary"), date.minusDays(4), null, null, null, TransactionSource.API, List.of(),
                "ivo", IVO);
        TransactionDto dinner = new TransactionDto(3L, TransactionType.EXPENSE, new BigDecimal("18.50"), Currency.EUR,
                "Dinner", List.of(7L, 11L), List.of("Dining/Restaurants", "Entertainment"), date, "Trattoria", PaymentMethod.CASH,
                "with friends", TransactionSource.INBOX_JSON, List.of(), "ana", ANA);
        TransactionDto pharmacy = new TransactionDto(4L, TransactionType.EXPENSE, new BigDecimal("400.00"), Currency.MKD,
                "Pharmacy", List.of(6L), List.of("Groceries"), date.plusDays(1), "Zegin", PaymentMethod.CARD, null,
                TransactionSource.INBOX_JSON, List.of(), "ana", ANA);

        return new MonthlyReport(
                YearMonth.of(2026, 10),
                "October 2026",
                List.of(IVO, ANA),
                OffsetDateTime.of(2026, 11, 2, 8, 0, 0, 0, ZoneOffset.UTC),
                List.of(new CurrencySummary(Currency.EUR, BigDecimal.ZERO, new BigDecimal("18.50"), new BigDecimal("-18.50"),
                                BigDecimal.ZERO, BigDecimal.ZERO, null),
                        new CurrencySummary(Currency.MKD, new BigDecimal("60000.00"), new BigDecimal("1650.00"), new BigDecimal("58350.00"),
                                new BigDecimal("55000.00"), new BigDecimal("1000.00"), new BigDecimal("65.0"))),
                List.of(new PersonSummary(IVO, Currency.MKD, new BigDecimal("60000.00"), new BigDecimal("1250.00"), new BigDecimal("58750.00")),
                        new PersonSummary(ANA, Currency.EUR, BigDecimal.ZERO, new BigDecimal("18.50"), new BigDecimal("-18.50")),
                        new PersonSummary(ANA, Currency.MKD, BigDecimal.ZERO, new BigDecimal("400.00"), new BigDecimal("-400.00"))),
                List.of(new CategoryTotal(Currency.EUR, "Dining/Restaurants", new BigDecimal("18.50"), 1, new BigDecimal("100.0"), split(null, "18.50")),
                        new CategoryTotal(Currency.EUR, "Entertainment", new BigDecimal("18.50"), 1, new BigDecimal("100.0"), split(null, "18.50")),
                        new CategoryTotal(Currency.MKD, "Groceries", new BigDecimal("1650.00"), 2, new BigDecimal("100.0"), split("1250.00", "400.00"))),
                List.of(new MerchantTotal(Currency.MKD, "Веро Центар", new BigDecimal("1250.00"), 1),
                        new MerchantTotal(Currency.MKD, "Zegin", new BigDecimal("400.00"), 1)),
                List.of(salary, groceries, dinner, pharmacy),
                true);
    }

    /** The same month with a single person: no per-person sections. */
    public static MonthlyReport singlePersonReport() {
        MonthlyReport full = sampleReport();
        return new MonthlyReport(full.month(), full.monthLabel(), List.of(IVO), full.generatedAt(),
                full.totals(), full.personTotals().subList(0, 1), full.categories(), full.topMerchants(),
                full.transactions().subList(0, 2), false);
    }

    private static Map<String, BigDecimal> split(String ivo, String ana) {
        Map<String, BigDecimal> byPerson = new LinkedHashMap<>();
        if (ivo != null) {
            byPerson.put(IVO, new BigDecimal(ivo));
        }
        if (ana != null) {
            byPerson.put(ANA, new BigDecimal(ana));
        }
        return byPerson;
    }
}
