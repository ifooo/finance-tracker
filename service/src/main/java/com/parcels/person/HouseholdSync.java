package com.parcels.person;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Applies pft.household.people to the database at startup, before the inbox is first read. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class HouseholdSync implements ApplicationRunner {

    private final PersonService personService;

    @Override
    public void run(ApplicationArguments args) {
        personService.syncFromConfig();
    }
}
