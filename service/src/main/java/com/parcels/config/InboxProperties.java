package com.parcels.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Settings for the inbox folder that hand-written JSON files are dropped into.
 *
 * @param enabled               whether the inbox is polled on a schedule
 * @param dir                   folder to watch; {@code processed/} and {@code failed/} are created inside it
 * @param pollInterval          delay between two scans
 * @param settleTime            files modified more recently than this are skipped (they may still be being written)
 * @param autoCreateCategories  create unknown categories instead of rejecting the file
 */
@ConfigurationProperties("pft.inbox")
public record InboxProperties(@DefaultValue("true") boolean enabled,
                              Path dir,
                              @DefaultValue("PT30S") Duration pollInterval,
                              @DefaultValue("PT5S") Duration settleTime,
                              @DefaultValue("false") boolean autoCreateCategories) {
}
