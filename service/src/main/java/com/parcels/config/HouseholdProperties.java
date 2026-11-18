package com.parcels.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * The people whose transactions are tracked. Set in ~/.config/finance-tracker/application.yml so names stay out of git.
 * People are created or updated at startup (matched by username); nobody is ever deleted.
 *
 * @param defaultPerson username used when a transaction does not say whose it is; defaults to the first person
 * @param people        the household members
 */
@Validated
@ConfigurationProperties("pft.household")
public record HouseholdProperties(String defaultPerson,
                                  @DefaultValue List<@Valid Person> people) {

    /**
     * @param username short id ("ivo"), accepted as a transaction's person: lowercase letters, digits, '-' or '_'
     * @param name     display name in reports
     */
    public record Person(@NotBlank @Pattern(regexp = "[a-z0-9_-]{1,50}") String username,
                         String name,
                         String email) {
    }
}
