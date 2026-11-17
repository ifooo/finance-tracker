package com.parcels.inbox;

import com.parcels.config.InboxProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.parcels.inbox.InboxOutcome.Status.DUPLICATE;
import static com.parcels.inbox.InboxOutcome.Status.FAILED;
import static com.parcels.inbox.InboxOutcome.Status.IMPORTED;

/**
 * Scans the inbox folder and hands each file to the matching {@link InboxFileHandler}.
 * <ul>
 *   <li>imported or duplicate → {@code processed/yyyy/MM/<timestamp>_<name>}</li>
 *   <li>invalid or unsupported → {@code failed/<timestamp>_<name>} plus {@code <same>.error.txt}</li>
 * </ul>
 * Hidden files, temp/partial downloads and files modified within the settle time are left alone.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InboxProcessor {

    static final String PROCESSED = "processed";
    static final String FAILED_DIR = "failed";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final List<String> IGNORED_SUFFIXES = List.of("~", ".tmp", ".part", ".crdownload", ".swp");

    private final List<InboxFileHandler> handlers;
    private final InboxProperties properties;
    private final Clock clock;

    /** Processes the configured inbox folder. */
    public List<InboxOutcome> processInbox() {
        Path dir = Objects.requireNonNull(properties.dir(), "pft.inbox.dir is not configured");
        return process(dir);
    }

    public List<InboxOutcome> process(Path inbox) {
        List<Path> files = readyFiles(inbox);
        List<InboxOutcome> outcomes = new ArrayList<>();
        for (Path file : files) {
            InboxOutcome outcome = processFile(inbox, file);
            outcomes.add(outcome);
            switch (outcome.status()) {
                case IMPORTED -> log.info("Inbox: imported {} transaction(s) from {}", outcome.transactionCount(), outcome.fileName());
                case DUPLICATE -> log.info("Inbox: {} was already imported, nothing stored", outcome.fileName());
                case FAILED -> log.warn("Inbox: {} failed: {}", outcome.fileName(), String.join("; ", outcome.errors()));
            }
        }
        return outcomes;
    }

    private InboxOutcome processFile(Path inbox, Path file) {
        String name = file.getFileName().toString();
        InboxFileHandler handler = handlers.stream().filter(h -> h.supports(file)).findFirst().orElse(null);
        if (handler == null) {
            String supported = handlers.stream().map(InboxFileHandler::description).collect(Collectors.joining(", "));
            return fail(inbox, file, List.of("Unsupported file type. Supported: " + supported));
        }
        InboxResult result;
        try {
            result = handler.process(file);
        } catch (InboxFileException e) {
            return fail(inbox, file, e.getErrors());
        } catch (RuntimeException e) {
            log.error("Inbox: unexpected error while processing {}", name, e);
            Throwable root = ExceptionUtils.getRootCause(e);
            return fail(inbox, file, List.of("Unexpected error: " + root.getClass().getSimpleName() + ": " + root.getMessage()));
        }
        Path target = null;
        try {
            target = moveTo(file, processedDir(inbox));
        } catch (UncheckedIOException e) {
            // Already stored; the next scan sees the same hash and only retries the move.
            log.error("Inbox: {} was processed but could not be moved to {}/", name, PROCESSED, e);
        }
        return new InboxOutcome(name, result.duplicate() ? DUPLICATE : IMPORTED, result.transactionCount(), List.of(), target);
    }

    private InboxOutcome fail(Path inbox, Path file, List<String> errors) {
        String name = file.getFileName().toString();
        Path target = null;
        try {
            target = moveTo(file, inbox.resolve(FAILED_DIR));
            Files.writeString(target.resolveSibling(target.getFileName() + ".error.txt"), errorReport(name, errors), StandardCharsets.UTF_8);
        } catch (UncheckedIOException | IOException e) {
            log.error("Inbox: could not move {} to {}/", name, FAILED_DIR, e);
        }
        return new InboxOutcome(name, FAILED, 0, errors, target);
    }

    private String errorReport(String fileName, List<String> errors) {
        StringBuilder report = new StringBuilder()
                .append("File:      ").append(fileName).append('\n')
                .append("Failed at: ").append(OffsetDateTime.now(clock)).append('\n')
                .append("Nothing from this file was stored. Fix the problems below and drop the file into the inbox again.\n\n");
        errors.forEach(error -> report.append("- ").append(error).append('\n'));
        return report.toString();
    }

    private List<Path> readyFiles(Path inbox) {
        try {
            Files.createDirectories(inbox);
            Instant settledBefore = clock.instant().minus(properties.settleTime());
            try (Stream<Path> entries = Files.list(inbox)) {
                return entries
                        .filter(Files::isRegularFile)
                        .filter(InboxProcessor::isCandidate)
                        .filter(file -> lastModified(file).isBefore(settledBefore))
                        .sorted()
                        .toList();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read inbox folder " + inbox, e);
        }
    }

    private static boolean isCandidate(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return !name.startsWith(".") && IGNORED_SUFFIXES.stream().noneMatch(name::endsWith);
    }

    private static Instant lastModified(Path file) {
        try {
            return Files.getLastModifiedTime(file).toInstant();
        } catch (IOException e) {
            return Instant.MAX; // vanished or unreadable: skip this round
        }
    }

    private Path processedDir(Path inbox) {
        LocalDateTime now = LocalDateTime.now(clock);
        return inbox.resolve(PROCESSED).resolve(String.valueOf(now.getYear())).resolve("%02d".formatted(now.getMonthValue()));
    }

    /** Moves the file into {@code dir} as {@code <timestamp>_<name>}, adding a counter if that name is taken. */
    private Path moveTo(Path file, Path dir) {
        try {
            Files.createDirectories(dir);
            String base = LocalDateTime.now(clock).format(STAMP) + "_" + file.getFileName();
            Path target = dir.resolve(base);
            for (int i = 2; Files.exists(target); i++) {
                target = dir.resolve(base.replaceFirst("(\\.[^.]*)?$", "-" + i + "$1"));
            }
            return Files.move(file, target);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not move " + file + " to " + dir, e);
        }
    }
}
