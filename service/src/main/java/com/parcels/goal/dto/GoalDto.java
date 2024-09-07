package com.parcels.goal.dto;

import java.time.OffsetDateTime;

public record GoalDto(Long id,
                      String fullName,
                      String goalName,
                      Double targetAmount,
                      Double currentAmount,
                      OffsetDateTime deadline) {
}
