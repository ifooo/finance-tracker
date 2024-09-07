package com.parcels.budget.dto;

import com.parcels.domain.enums.Period;

import java.time.OffsetDateTime;

public record BudgetUpdateCommand(Long id,
                                  Long userId,
                                  Long categoryId,
                                  Double amount,
                                  OffsetDateTime startDate,
                                  Period period) {
}
