package com.parcels.inbox;

import com.parcels.config.InboxProperties;
import com.parcels.domain.Category;
import com.parcels.domain.ImportFile;
import com.parcels.domain.Transaction;
import com.parcels.domain.TransactionItem;
import com.parcels.domain.UserAccount;
import com.parcels.domain.enums.TransactionSource;
import com.parcels.inbox.dto.InboxFile;
import com.parcels.inbox.dto.InboxItem;
import com.parcels.inbox.dto.InboxTransaction;
import com.parcels.person.PersonLookup;
import com.parcels.person.PersonService;
import com.parcels.person.UnknownPersonException;
import com.parcels.report.ReportDataChangedEvent;
import com.parcels.repository.CategoryRepository;
import com.parcels.repository.ImportFileRepository;
import com.parcels.repository.TransactionRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.TokenStreamLocation;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Imports hand-written {@code *.json} files (format: {@link InboxFile}). A file is imported completely or not at all.
 */
@Component
@Slf4j
public class JsonTransactionFileHandler implements InboxFileHandler {

    private final ObjectReader reader;
    private final Validator validator;
    private final InboxProperties properties;
    private final ImportFileRepository importFileRepository;
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final PersonService personService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public JsonTransactionFileHandler(JsonMapper jsonMapper,
                                      Validator validator,
                                      InboxProperties properties,
                                      ImportFileRepository importFileRepository,
                                      TransactionRepository transactionRepository,
                                      CategoryRepository categoryRepository,
                                      PersonService personService,
                                      ApplicationEventPublisher events,
                                      Clock clock) {
        // Hand-written files: reject misspelled properties instead of silently ignoring them, accept "expense" for EXPENSE.
        this.reader = jsonMapper.rebuild()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
                .build()
                .readerFor(InboxFile.class);
        this.validator = validator;
        this.properties = properties;
        this.importFileRepository = importFileRepository;
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
        this.personService = personService;
        this.events = events;
        this.clock = clock;
    }

    @Override
    public boolean supports(Path file) {
        return file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json");
    }

    @Override
    public String description() {
        return "*.json transaction files";
    }

    @Override
    @Transactional
    public InboxResult process(Path file) {
        byte[] content = read(file);
        String sha256 = sha256(content);
        if (importFileRepository.findBySha256(sha256).isPresent()) {
            return InboxResult.alreadyImported();
        }

        InboxFile inboxFile = parse(content);
        validate(inboxFile);
        List<Transaction> transactions = toTransactions(inboxFile);

        ImportFile importFile = importFileRepository.save(ImportFile.builder()
                .fileName(file.getFileName().toString())
                .sha256(sha256)
                .transactionCount(transactions.size())
                .importedAt(OffsetDateTime.now(clock))
                .build());
        transactions.forEach(t -> t.setImportFile(importFile));
        transactionRepository.saveAll(transactions);
        events.publishEvent(ReportDataChangedEvent.forDates(transactions.stream().map(Transaction::getDateFrom).toList(), clock.getZone()));
        return InboxResult.imported(transactions.size());
    }

    private InboxFile parse(byte[] content) {
        try {
            InboxFile parsed = reader.readValue(content);
            if (parsed == null) {
                throw new InboxFileException("the file is empty");
            }
            return parsed;
        } catch (JacksonException e) {
            throw new InboxFileException(describe(e));
        }
    }

    private void validate(InboxFile inboxFile) {
        List<String> errors = validator.validate(inboxFile).stream()
                .map(JsonTransactionFileHandler::describe)
                .sorted()
                .toList();
        if (!errors.isEmpty()) {
            throw new InboxFileException(errors);
        }
    }

    /**
     * Builds the entities, collecting every problem (bad date, unknown category, duplicate id) before failing.
     */
    private List<Transaction> toTransactions(InboxFile inboxFile) {
        List<InboxTransaction> entries = inboxFile.transactions();
        ZoneId zone = clock.getZone();
        PersonLookup people = personService.lookup();
        List<String> errors = new ArrayList<>();
        UserAccount fileDefault = resolvePerson(people, inboxFile.person(), null, "person", errors);
        CategoryLookup categories = new CategoryLookup(categoryRepository.findAll());
        Set<String> externalIdsInFile = new HashSet<>();

        List<Transaction> transactions = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            InboxTransaction entry = entries.get(i);
            String path = "transactions[" + i + "]";

            OffsetDateTime date = parseDate(entry.date(), zone);
            if (date == null) {
                errors.add(path + ".date: expected yyyy-MM-dd or an ISO-8601 date-time, got \"" + entry.date() + "\"");
            }

            String externalId = blankToNull(entry.externalId());
            if (externalId != null) {
                if (!externalIdsInFile.add(externalId)) {
                    errors.add(path + ".externalId: \"" + externalId + "\" is used more than once in this file");
                } else if (transactionRepository.existsByExternalId(externalId)) {
                    errors.add(path + ".externalId: \"" + externalId + "\" was already imported");
                }
            }

            UserAccount person = resolvePerson(people, entry.person(), fileDefault, path + ".person", errors);
            List<Category> resolved = resolveCategories(entry.categories(), categories, path, errors);
            List<TransactionItem> items = toItems(entry.items(), path, errors);

            Transaction transaction = Transaction.builder()
                    .transactionType(entry.type())
                    .amount(entry.amount())
                    .currency(entry.currency())
                    .dateFrom(date)
                    .merchant(blankToNull(entry.merchant()))
                    .description(blankToNull(entry.description()))
                    .paymentMethod(entry.paymentMethod())
                    .notes(blankToNull(entry.notes()))
                    .externalId(externalId)
                    .source(TransactionSource.INBOX_JSON)
                    .userAccount(person)
                    .categories(resolved)
                    .build();
            items.forEach(transaction::addItem);
            transactions.add(transaction);
        }
        if (!errors.isEmpty()) {
            throw new InboxFileException(errors);
        }
        return transactions;
    }

    private List<Category> resolveCategories(List<String> names, CategoryLookup lookup, String path, List<String> errors) {
        Set<Category> resolved = new LinkedHashSet<>();
        for (String name : names) {
            Optional<Category> found = lookup.find(name);
            if (found.isPresent()) {
                resolved.add(found.get());
            } else if (properties.autoCreateCategories()) {
                Category created = categoryRepository.save(Category.builder().name(name.trim()).build());
                log.info("Created category '{}' from inbox file", created.getName());
                lookup.add(created);
                resolved.add(created);
            } else {
                errors.add(path + ".categories: unknown category \"" + name + "\". Known categories: "
                        + String.join(", ", lookup.knownNames())
                        + " (or set pft.inbox.auto-create-categories=true)");
            }
        }
        return new ArrayList<>(resolved);
    }

    /** Entry value, else the file-level value, else the household default. Records an error for an unknown name. */
    private static UserAccount resolvePerson(PersonLookup people, String name, UserAccount fallback,
                                             String path, List<String> errors) {
        if ((name == null || name.isBlank()) && fallback != null) {
            return fallback;
        }
        try {
            return people.resolve(name);
        } catch (UnknownPersonException e) {
            errors.add(path + ": " + e.getMessage());
            return fallback;
        }
    }

    private static List<TransactionItem> toItems(List<InboxItem> items, String path, List<String> errors) {
        if (items == null) {
            return List.of();
        }
        List<TransactionItem> result = new ArrayList<>();
        for (int j = 0; j < items.size(); j++) {
            InboxItem item = items.get(j);
            BigDecimal total = item.total();
            if (total == null && item.quantity() != null && item.unitPrice() != null) {
                total = item.quantity().multiply(item.unitPrice()).setScale(2, RoundingMode.HALF_UP);
            }
            if (total == null) {
                errors.add(path + ".items[" + j + "]: give either \"total\" or both \"quantity\" and \"unitPrice\"");
                continue;
            }
            result.add(TransactionItem.builder()
                    .name(item.name().trim())
                    .quantity(item.quantity())
                    .unitPrice(item.unitPrice())
                    .total(total)
                    .build());
        }
        return result;
    }

    /** A plain date means the start of that day in the local zone. Returns null when the value cannot be parsed. */
    static OffsetDateTime parseDate(String raw, ZoneId zone) {
        String value = raw.trim();
        try {
            return LocalDate.parse(value).atStartOfDay(zone).toOffsetDateTime();
        } catch (DateTimeParseException ignored) {
            // not a plain date
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            // no offset
        }
        try {
            return LocalDateTime.parse(value).atZone(zone).toOffsetDateTime();
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    private static String describe(ConstraintViolation<?> violation) {
        return violation.getPropertyPath() + ": " + violation.getMessage();
    }

    /** "transactions[1].amount: Cannot deserialize ... (line 7, column 17)", without Java package names. */
    private static String describe(JacksonException e) {
        StringBuilder path = new StringBuilder();
        for (JacksonException.Reference reference : e.getPath()) {
            if (reference.getPropertyName() != null) {
                if (!path.isEmpty()) {
                    path.append('.');
                }
                path.append(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                path.append('[').append(reference.getIndex()).append(']');
            }
        }
        String message = String.valueOf(e.getOriginalMessage())
                .replaceAll("(?:[a-z][a-z0-9_]*\\.)+([A-Z]\\w*)", "$1")
                .replaceAll("\\s+", " ")
                .trim();
        TokenStreamLocation location = e.getLocation();
        if (location != null && location.getLineNr() > 0) {
            message += " (line " + location.getLineNr() + ", column " + location.getColumnNr() + ")";
        }
        return path.isEmpty() ? message : path + ": " + message;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static byte[] read(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + file, e);
        }
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
