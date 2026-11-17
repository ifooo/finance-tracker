package com.parcels.inbox;

import lombok.Getter;

import java.util.List;

/**
 * The content of an inbox file is invalid. Each error is a readable line, prefixed with the JSON path when known.
 */
@Getter
public class InboxFileException extends RuntimeException {

    private final List<String> errors;

    public InboxFileException(List<String> errors) {
        super(String.join("; ", errors));
        this.errors = List.copyOf(errors);
    }

    public InboxFileException(String error) {
        this(List.of(error));
    }
}
