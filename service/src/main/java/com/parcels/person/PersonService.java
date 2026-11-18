package com.parcels.person;

import com.parcels.config.HouseholdProperties;
import com.parcels.domain.UserAccount;
import com.parcels.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Knows the household members: resolves the "person" of a transaction and picks the default person.
 */
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PersonService {

    private final UserAccountRepository userAccountRepository;
    private final HouseholdProperties properties;

    /** Creates or updates the configured people (matched by username). */
    public void syncFromConfig() {
        for (HouseholdProperties.Person person : properties.people()) {
            UserAccount account = userAccountRepository.findByUsernameIgnoreCase(person.username())
                    .orElseGet(() -> UserAccount.builder().username(person.username()).build());
            boolean isNew = account.getId() == null;
            if (person.name() != null) {
                account.setFullName(person.name());
            }
            if (person.email() != null) {
                account.setEmail(person.email());
            }
            userAccountRepository.save(account);
            if (isNew) {
                log.info("Household: added {} ({})", account.displayName(), account.getUsername());
            }
        }
    }

    @Transactional(readOnly = true)
    public List<UserAccount> all() {
        return userAccountRepository.findAllByOrderByIdAsc();
    }

    /** A lookup over all people, for resolving many names at once. */
    @Transactional(readOnly = true)
    public PersonLookup lookup() {
        return new PersonLookup(all(), defaultPerson());
    }

    /**
     * The person for {@code name} (username, full name or first name, ignoring case); the default person when blank.
     *
     * @throws UnknownPersonException when nobody matches
     */
    @Transactional(readOnly = true)
    public UserAccount resolve(String name) {
        return lookup().resolve(name);
    }

    private UserAccount defaultPerson() {
        String configured = StringUtils.firstNonBlank(properties.defaultPerson(),
                properties.people().isEmpty() ? null : properties.people().getFirst().username());
        if (configured != null) {
            return userAccountRepository.findByUsernameIgnoreCase(configured)
                    .orElseThrow(() -> new IllegalStateException("pft.household.default-person '" + configured + "' does not exist"));
        }
        return all().stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("No people configured (pft.household.people)"));
    }
}
