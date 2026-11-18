package com.parcels.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties({ReportProperties.class, HouseholdProperties.class})
public class PftConfiguration {

    /** System clock in the local zone; report month boundaries use this zone. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
