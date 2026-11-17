package com.parcels.person;

import com.parcels.domain.UserAccount;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves a hand-written person: username, full name or first name, ignoring case. A first name shared by two
 * people is ambiguous and not used.
 */
public class PersonLookup {

    private final List<UserAccount> people;
    private final UserAccount defaultPerson;
    private final Map<String, UserAccount> byKey = new HashMap<>();

    PersonLookup(List<UserAccount> people, UserAccount defaultPerson) {
        this.people = List.copyOf(people);
        this.defaultPerson = defaultPerson;
        Map<String, UserAccount> firstNames = new HashMap<>();
        Set<String> ambiguous = new HashSet<>();
        for (UserAccount person : people) {
            if (person.getFullName() != null && !person.getFullName().isBlank()) {
                String first = key(person.getFullName().trim().split("\\s+")[0]);
                if (firstNames.putIfAbsent(first, person) != null) {
                    ambiguous.add(first);
                }
            }
        }
        ambiguous.forEach(firstNames::remove);
        byKey.putAll(firstNames);
        for (UserAccount person : people) {
            if (person.getFullName() != null && !person.getFullName().isBlank()) {
                byKey.put(key(person.getFullName()), person);
            }
        }
        people.forEach(person -> byKey.put(key(person.getUsername()), person)); // usernames win
    }

    public UserAccount resolve(String name) {
        if (name == null || name.isBlank()) {
            return defaultPerson;
        }
        UserAccount person = byKey.get(key(name));
        if (person == null) {
            throw new UnknownPersonException(name, describeKnown());
        }
        return person;
    }

    public String describeKnown() {
        return people.stream()
                .map(p -> p.getFullName() == null || p.getFullName().isBlank() ? p.getUsername() : p.getUsername() + " (" + p.getFullName() + ")")
                .collect(Collectors.joining(", "));
    }

    private static String key(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
