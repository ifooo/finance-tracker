package com.parcels.transaction.dto;

import com.parcels.domain.enums.Currency;
import com.parcels.domain.enums.TransactionType;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record TransactionUpdateCommand(Long id,
                                       @NotNull TransactionType type,
                                       @NotNull Double amount,
                                       @NotNull Currency currency,
                                       String description,
                                       Long categoryId,
                                       @NotNull OffsetDateTime datetime) {
}
