package com.parcels.report.dto;

import com.parcels.domain.enums.Currency;

import java.math.BigDecimal;

/**
 * Totals for one currency (amounts in different currencies are never added together).
 *
 * @param expenseChangePercent change in expenses against the previous month; null when there were none
 */
public record CurrencySummary(Currency currency,
                              BigDecimal income,
                              BigDecimal expense,
                              BigDecimal net,
                              BigDecimal previousIncome,
                              BigDecimal previousExpense,
                              BigDecimal expenseChangePercent) {
}
