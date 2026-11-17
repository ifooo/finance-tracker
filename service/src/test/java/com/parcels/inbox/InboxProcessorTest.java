package com.parcels.inbox;

import com.parcels.domain.Category;
import com.parcels.infrastucture.extended.BaseDbTest;
import com.parcels.repository.CategoryRepository;
import com.parcels.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class InboxProcessorTest extends BaseDbTest {

    private static final String VALID = """
            { "transactions": [ { "type": "EXPENSE", "date": "2026-11-15", "amount": 12.5, "currency": "MKD", "categories": ["Groceries"] } ] }
            """;

    @TempDir
    Path inbox;

    @Autowired
    private InboxProcessor processor;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        categoryRepository.save(Category.builder().name("Groceries").build());
    }

    @Test
    void importedFileIsMovedToProcessed() throws IOException {
        settled("groceries.json", VALID);

        List<InboxOutcome> outcomes = processor.process(inbox);

        assertThat(outcomes).singleElement().satisfies(o -> {
            assertThat(o.status()).isEqualTo(InboxOutcome.Status.IMPORTED);
            assertThat(o.transactionCount()).isEqualTo(1);
            assertThat(inbox.relativize(o.movedTo()).toString()).matches("processed/\\d{4}/\\d{2}/\\d{8}-\\d{6}_groceries\\.json");
        });
        assertThat(inbox.resolve("groceries.json")).doesNotExist();
        assertThat(transactionRepository.count()).isEqualTo(1);
    }

    @Test
    void duplicateFileIsMovedToProcessedWithoutStoringAgain() throws IOException {
        settled("a.json", VALID);
        processor.process(inbox);
        settled("a.json", VALID);

        assertThat(processor.process(inbox)).singleElement()
                .satisfies(o -> assertThat(o.status()).isEqualTo(InboxOutcome.Status.DUPLICATE));
        assertThat(transactionRepository.count()).isEqualTo(1);
        assertThat(filesUnder(inbox.resolve("processed"))).hasSize(2);
    }

    @Test
    void invalidFileIsMovedToFailedWithErrorReport() throws IOException {
        settled("bad.json", VALID.replace("Groceries", "Groceris"));

        InboxOutcome outcome = processor.process(inbox).getFirst();

        assertThat(outcome.status()).isEqualTo(InboxOutcome.Status.FAILED);
        assertThat(outcome.movedTo().getParent()).isEqualTo(inbox.resolve("failed"));
        Path report = outcome.movedTo().resolveSibling(outcome.movedTo().getFileName() + ".error.txt");
        assertThat(Files.readString(report))
                .contains("File:      bad.json")
                .contains("Nothing from this file was stored")
                .contains("- transactions[0].categories: unknown category \"Groceris\"");
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void unsupportedFileIsMovedToFailed() throws IOException {
        settled("receipt.jpg", "not really an image");

        assertThat(processor.process(inbox)).singleElement().satisfies(o -> {
            assertThat(o.status()).isEqualTo(InboxOutcome.Status.FAILED);
            assertThat(o.errors()).containsExactly("Unsupported file type. Supported: *.json transaction files");
        });
    }

    @Test
    void recentlyModifiedHiddenAndTempFilesAreLeftAlone() throws IOException {
        Files.writeString(inbox.resolve("still-writing.json"), VALID); // modified just now
        settled(".hidden.json", VALID);
        settled("download.json.part", VALID);
        settled("backup.json~", VALID);

        assertThat(processor.process(inbox)).isEmpty();
        assertThat(inbox.resolve("still-writing.json")).exists();
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void filesAreProcessedInNameOrderAndFoldersAreIgnored() throws IOException {
        settled("2.json", VALID.replace("12.5", "2"));
        settled("1.json", VALID.replace("12.5", "1"));
        Files.createDirectories(inbox.resolve("some-folder.json"));

        assertThat(processor.process(inbox)).extracting(InboxOutcome::fileName).containsExactly("1.json", "2.json");
    }

    /** Writes a file whose modification time is safely past the settle time. */
    private void settled(String name, String content) throws IOException {
        Path file = Files.writeString(inbox.resolve(name), content);
        Files.setLastModifiedTime(file, FileTime.from(Instant.now().minus(1, ChronoUnit.MINUTES)));
    }

    private static List<Path> filesUnder(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
            return walk.filter(Files::isRegularFile).toList();
        }
    }
}
