package com.parcels.endpoint.dto.out;

import java.time.OffsetDateTime;

public record BudgetOut(Long id,
                        String user,
                        String categoryName,
                        Double amount,
                        OffsetDateTime startDate,
                        OffsetDateTime endDate) {
}
