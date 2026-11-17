package com.parcels.inbox.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Root of a hand-written inbox JSON file. See {@code docs/inbox/transactions.schema.json}.
 *
 * @param schema optional {@code $schema} pointer for editor autocomplete; ignored by the app
 * @param person whose transactions these are, unless an entry says otherwise; the household default when left out
 */
public record InboxFile(@JsonProperty("$schema") String schema,
                        String person,
                        @NotEmpty List<@Valid InboxTransaction> transactions) {
}
