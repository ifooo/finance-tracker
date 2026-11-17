package com.parcels.report.dto;

import com.parcels.domain.enums.Currency;

import java.math.BigDecimal;

public record MerchantTotal(Currency currency, String merchant, BigDecimal amount, int count) {
}
