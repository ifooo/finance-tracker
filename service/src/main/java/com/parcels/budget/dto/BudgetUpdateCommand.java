package com.parcels.budget.dto;

import com.parcels.domain.enums.Period;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BudgetUpdateCommand(Long id,
                                  Long userId,
                                  @NotNull Long categoryId,
                                  BigDecimal amount,
                                  @NotNull OffsetDateTime startDate,
                                  @NotNull Period period) {
}
