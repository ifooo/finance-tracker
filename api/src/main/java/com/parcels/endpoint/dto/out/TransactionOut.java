package com.parcels.endpoint.dto.out;

import com.parcels.domain.enums.Currency;
import com.parcels.domain.enums.PaymentMethod;
import com.parcels.domain.enums.TransactionSource;
import com.parcels.domain.enums.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record TransactionOut(Long id,
                             TransactionType transactionType,
                             BigDecimal amount,
                             Currency currency,
                             String description,
                             List<Long> categoryIds,
                             List<String> categoryNames,
                             OffsetDateTime dateFrom,
                             String merchant,
                             PaymentMethod paymentMethod,
                             String notes,
                             TransactionSource source,
                             String person) {
}
