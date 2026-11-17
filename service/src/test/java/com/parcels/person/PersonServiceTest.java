package com.parcels.person;

import com.parcels.domain.UserAccount;
import com.parcels.infrastucture.extended.BaseDbTest;
import com.parcels.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PersonServiceTest extends BaseDbTest {

    @DynamicPropertySource
    static void household(DynamicPropertyRegistry registry) {
        registry.add("pft.household.default-person", () -> "mira");
        registry.add("pft.household.people[0].username", () -> "ivo");
        registry.add("pft.household.people[0].name", () -> "Ivo Configured");
        registry.add("pft.household.people[1].username", () -> "mira");
        registry.add("pft.household.people[1].name", () -> "Mira Test");
    }

    @Autowired
    private PersonService personService;
    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    void configuredPeopleAreSyncedAtStartup() {
        // The V8 account (username "ivo" since V17) is matched and renamed, not duplicated; "mira" is created with a fresh id.
        assertThat(personService.all()).extracting(UserAccount::getUsername).containsExactly("ivo", "mira");
        assertThat(userAccountRepository.findById(1L).orElseThrow().getFullName()).isEqualTo("Ivo Configured");
        assertThat(userAccountRepository.findByUsernameIgnoreCase("mira").orElseThrow().getId()).isGreaterThan(1L);
    }

    @Test
    void syncIsIdempotent() {
        personService.syncFromConfig();
        personService.syncFromConfig();

        assertThat(userAccountRepository.count()).isEqualTo(2);
    }

    @Test
    void resolvesByUsernameFullNameOrFirstNameIgnoringCase() {
        assertThat(personService.resolve("MIRA").getUsername()).isEqualTo("mira");
        assertThat(personService.resolve("mira test").getUsername()).isEqualTo("mira");
        assertThat(personService.resolve("Ivo").getUsername()).isEqualTo("ivo");
    }

    @Test
    void blankMeansTheConfiguredDefaultPerson() {
        assertThat(personService.resolve(null).getUsername()).isEqualTo("mira");
        assertThat(personService.resolve("  ").getUsername()).isEqualTo("mira");
    }

    @Test
    void sharedFirstNameIsAmbiguousButUsernameStillWorks() {
        userAccountRepository.save(UserAccount.builder().username("mira2").fullName("Mira Other").build());

        assertThatThrownBy(() -> personService.resolve("Mira X")).isInstanceOf(UnknownPersonException.class);
        assertThat(personService.resolve("mira").getUsername()).isEqualTo("mira"); // username wins
        assertThatThrownBy(() -> personService.lookup().resolve("Mira Unknown")).isInstanceOf(UnknownPersonException.class);
        assertThat(personService.resolve("Mira Other").getUsername()).isEqualTo("mira2");
    }
}
