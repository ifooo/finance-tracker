package com.parcels.goal.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record GoalDto(Long id,
                      String fullName,
                      String goalName,
                      BigDecimal targetAmount,
                      BigDecimal currentAmount,
                      OffsetDateTime deadline) {
}
