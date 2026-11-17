package com.parcels.inbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the inbox folder, starting shortly after startup. Disable with {@code pft.inbox.enabled=false}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "pft.inbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class InboxScheduler {

    private final InboxProcessor processor;

    @Scheduled(initialDelayString = "PT2S", fixedDelayString = "${pft.inbox.poll-interval:PT30S}")
    public void poll() {
        try {
            processor.processInbox();
        } catch (RuntimeException e) {
            log.error("Inbox: scan failed", e);
        }
    }
}
