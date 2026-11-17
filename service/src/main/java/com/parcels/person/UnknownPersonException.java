package com.parcels.person;

import jakarta.validation.ValidationException;

/** No household member matches a "person" value. A {@link ValidationException}, so the API answers 400. */
public class UnknownPersonException extends ValidationException {

    public UnknownPersonException(String name, String known) {
        super("unknown person \"" + name + "\". Known people: " + known + " (add more under pft.household.people)");
    }
}
