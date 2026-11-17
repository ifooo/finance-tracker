package com.parcels.inbox.dto;

import com.parcels.domain.enums.Currency;
import com.parcels.domain.enums.PaymentMethod;
import com.parcels.domain.enums.TransactionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * One transaction in an inbox file.
 *
 * @param date       {@code yyyy-MM-dd} (start of that day, local zone) or an ISO-8601 date-time
 * @param categories category names, matched case-insensitively (also by the part before "(" or either side of "/")
 * @param externalId optional unique id; an entry whose id was already imported is rejected
 * @param person     username, full name or first name; overrides the file-level person
 */
public record InboxTransaction(@Size(max = 100) String externalId,
                               String person,
                               @NotNull TransactionType type,
                               @NotBlank String date,
                               @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
                               @NotNull Currency currency,
                               @Size(max = 255) String merchant,
                               @Size(max = 255) String description,
                               @NotEmpty List<@NotBlank String> categories,
                               PaymentMethod paymentMethod,
                               String notes,
                               List<@Valid InboxItem> items) {
}
