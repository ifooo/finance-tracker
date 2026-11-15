package com.parcels.endpoint.dto.out;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record GoalOut(Long id,
                      String fullName,
                      String goalName,
                      BigDecimal targetAmount,
                      BigDecimal currentAmount,
                      OffsetDateTime deadline) {
}
