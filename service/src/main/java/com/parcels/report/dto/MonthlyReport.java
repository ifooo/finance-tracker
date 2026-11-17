package com.parcels.report.dto;

import com.parcels.transaction.dto.TransactionDto;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Everything the monthly spreadsheet and PDF show. Amounts are never added across currencies.
 *
 * @param monthLabel                e.g. "October 2026"
 * @param people                    display names of the people with transactions this month, household order
 * @param totals                    household totals per currency
 * @param personTotals              totals per person and currency
 * @param multiCategoryTransactions some transactions have several categories, so category totals overlap
 * @param transactions              all transactions of the month, oldest first
 */
public record MonthlyReport(YearMonth month,
                            String monthLabel,
                            List<String> people,
                            OffsetDateTime generatedAt,
                            List<CurrencySummary> totals,
                            List<PersonSummary> personTotals,
                            List<CategoryTotal> categories,
                            List<MerchantTotal> topMerchants,
                            List<TransactionDto> transactions,
                            boolean multiCategoryTransactions) {

    /** True when more than one person has transactions this month, i.e. a per-person split is worth showing. */
    public boolean household() {
        return people.size() > 1;
    }
}
