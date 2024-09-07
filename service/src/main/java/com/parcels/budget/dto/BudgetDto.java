package com.parcels.budget.dto;

import java.time.OffsetDateTime;

public record BudgetDto(Long id,
                        String user,
                        String categoryName,
                        Double amount,
                        OffsetDateTime startDate,
                        OffsetDateTime endDate) {
}
