package com.parcels.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({InboxProperties.class, ReportProperties.class, HouseholdProperties.class})
public class PftConfiguration {

    /** System clock in the local zone; month boundaries and plain dates in inbox files use this zone. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
