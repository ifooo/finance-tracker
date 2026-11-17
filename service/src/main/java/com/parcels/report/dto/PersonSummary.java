package com.parcels.report.dto;

import com.parcels.domain.enums.Currency;

import java.math.BigDecimal;

/** One person's totals in one currency for the month. */
public record PersonSummary(String person, Currency currency, BigDecimal income, BigDecimal expense, BigDecimal net) {
}
