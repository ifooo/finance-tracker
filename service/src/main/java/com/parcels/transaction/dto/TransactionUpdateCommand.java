package com.parcels.transaction.dto;

import com.parcels.domain.enums.Currency;
import com.parcels.domain.enums.TransactionType;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;

public record TransactionUpdateCommand(Long id,
                                       @NotNull TransactionType type,
                                       @NotNull Double amount,
                                       @NotNull Currency currency,
                                       @NotNull String description,
                                       @NotNull List<Long> categoryIds,
                                       @NotNull OffsetDateTime datetime) {
}
