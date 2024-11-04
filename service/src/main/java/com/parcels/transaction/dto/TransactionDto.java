package com.parcels.transaction.dto;

import com.parcels.domain.enums.Currency;
import com.parcels.domain.enums.TransactionType;

import java.time.OffsetDateTime;
import java.util.List;

public record TransactionDto(Long id,
                             TransactionType transactionType,
                             Double amount,
                             Currency currency,
                             String description,
                             List<Long> categoryIds,
                             OffsetDateTime dateFrom) {
}
