package com.parcels.endpoint.dto.out;

import com.parcels.domain.enums.Currency;
import com.parcels.domain.enums.TransactionType;

import java.time.OffsetDateTime;

public record TransactionOut(Long id,
                             TransactionType transactionType,
                             Double amount,
                             Currency currency,
                             String description,
                             String category,
                             OffsetDateTime dateFrom) {
}
