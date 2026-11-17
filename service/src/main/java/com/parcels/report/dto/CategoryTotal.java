package com.parcels.report.dto;

import com.parcels.domain.enums.Currency;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Expenses of one category in one currency.
 *
 * @param sharePercent share of all expenses in this currency
 * @param byPerson     the amount per person (display name), in the order of {@link MonthlyReport#people()};
 *                     people without spending in this category are left out
 */
public record CategoryTotal(Currency currency, String category, BigDecimal amount, int count, BigDecimal sharePercent,
                            Map<String, BigDecimal> byPerson) {
}
