package com.parcels.endpoint.dto.out;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BudgetOut(Long id,
                        String user,
                        String categoryName,
                        BigDecimal amount,
                        OffsetDateTime startDate,
                        OffsetDateTime endDate) {
}
