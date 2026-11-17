package com.parcels.inbox.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * A receipt line. {@code total} may be left out when both {@code quantity} and {@code unitPrice} are given.
 */
public record InboxItem(@NotBlank @Size(max = 255) String name,
                        @Positive @Digits(integer = 16, fraction = 3) BigDecimal quantity,
                        @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal unitPrice,
                        @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal total) {
}
