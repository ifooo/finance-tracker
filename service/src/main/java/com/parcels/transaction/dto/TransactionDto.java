package com.parcels.transaction.dto;

import com.parcels.domain.enums.Currency;
import com.parcels.domain.enums.TransactionType;

import java.time.OffsetDateTime;

public record TransactionDto(Long id,
                             TransactionType transactionType,
                             Double amount,
                             Currency currency,
                             String description,
                             String category,
                             OffsetDateTime dateFrom) {
}
