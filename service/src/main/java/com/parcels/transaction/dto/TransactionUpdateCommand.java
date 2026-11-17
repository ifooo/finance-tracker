package com.parcels.transaction.dto;

import com.parcels.domain.enums.Currency;
import com.parcels.domain.enums.TransactionType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * @param person username, full name or first name of a household member; the default person when left out
 */
public record TransactionUpdateCommand(Long id,
                                       @NotNull TransactionType type,
                                       @NotNull BigDecimal amount,
                                       @NotNull Currency currency,
                                       @NotNull String description,
                                       @NotNull List<Long> categoryIds,
                                       @NotNull OffsetDateTime datetime,
                                       String person) {
}
