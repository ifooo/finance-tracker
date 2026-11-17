package com.parcels.inbox;

import java.nio.file.Path;
import java.util.List;

/**
 * What happened to one inbox file.
 *
 * @param movedTo where the file ended up ({@code processed/...} or {@code failed/...}); null when it could not be moved
 */
public record InboxOutcome(String fileName, Status status, int transactionCount, List<String> errors, Path movedTo) {

    public enum Status {
        IMPORTED,
        DUPLICATE,
        FAILED
    }
}
