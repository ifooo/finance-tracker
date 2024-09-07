package com.parcels.endpoint.dto.out;

import java.time.OffsetDateTime;

public record GoalOut(Long id,
                      String fullName,
                      String goalName,
                      Double targetAmount,
                      Double currentAmount,
                      OffsetDateTime deadline) {
}
