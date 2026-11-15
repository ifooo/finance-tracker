package com.parcels.budget.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BudgetDto(Long id,
                        String user,
                        String categoryName,
                        BigDecimal amount,
                        OffsetDateTime startDate,
                        OffsetDateTime endDate) {
}
