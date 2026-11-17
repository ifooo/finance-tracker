package com.parcels.transaction;

import com.parcels.domain.Category;
import com.parcels.domain.Transaction;
import com.parcels.domain.enums.TransactionType;
import com.parcels.report.ReportDataChangedEvent;
import com.parcels.repository.CategoryRepository;
import com.parcels.repository.TransactionRepository;
import com.parcels.person.PersonService;
import com.parcels.transaction.dto.TransactionDto;
import com.parcels.transaction.dto.TransactionUpdateCommand;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static com.parcels.repository.TransactionSpecifications.dateOnOrAfter;
import static com.parcels.repository.TransactionSpecifications.dateOnOrBefore;
import static com.parcels.repository.TransactionSpecifications.hasType;

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    /** Newest first; id breaks ties so pages are stable when several transactions share a date. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("dateFrom"), Sort.Order.desc("id"));

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final ConversionService conversionService;
    private final PersonService personService;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Override
    public TransactionDto getById(Long id) {
        return conversionService.convert(findTransaction(id), TransactionDto.class);
    }

    @Override
    public TransactionDto save(TransactionUpdateCommand transactionPersistCommand) {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(transactionPersistCommand.type());
        if (transactionPersistCommand.categoryIds() != null && !transactionPersistCommand.categoryIds().isEmpty()) {
            transaction.setCategories(findCategories(transactionPersistCommand.categoryIds()));
        }
        transaction.setAmount(transactionPersistCommand.amount());
        transaction.setDescription(transactionPersistCommand.description());
        transaction.setDateFrom(transactionPersistCommand.datetime());
        transaction.setCurrency(transactionPersistCommand.currency());
        transaction.setUserAccount(personService.resolve(transactionPersistCommand.person()));

        Transaction saved = transactionRepository.save(transaction);
        events.publishEvent(ReportDataChangedEvent.forDates(Collections.singletonList(saved.getDateFrom()), clock.getZone()));

        TransactionDto result = conversionService.convert(saved, TransactionDto.class);
        return result;
    }

    @Override
    public Boolean delete(Long id) {
        Transaction transaction = findTransaction(id);
        transactionRepository.delete(transaction);
        events.publishEvent(ReportDataChangedEvent.forDates(Collections.singletonList(transaction.getDateFrom()), clock.getZone()));
        return true;
    }

    @Override
    public Page<TransactionDto> getTransactionsByTypeAndDateRange(TransactionType transactionType, OffsetDateTime from, OffsetDateTime to, int page, int size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("'from' (" + from + ") must not be after 'to' (" + to + ")");
        }
        Specification<Transaction> spec = hasType(transactionType);
        if (from != null) {
            spec = spec.and(dateOnOrAfter(from));
        }
        if (to != null) {
            spec = spec.and(dateOnOrBefore(to));
        }
        Pageable pageRequest = PageRequest.of(page, size, NEWEST_FIRST);
        Page<Transaction> result = transactionRepository.findAll(spec, pageRequest);
        return result.map(transaction -> conversionService.convert(transaction, TransactionDto.class));
    }

    /**
     * Loads all requested categories, failing if any id does not exist (findAllById silently skips missing ones).
     */
    private List<Category> findCategories(List<Long> categoryIds) {
        Set<Long> requested = new LinkedHashSet<>(categoryIds);
        List<Category> found = categoryRepository.findAllById(requested);
        if (found.size() != requested.size()) {
            found.forEach(category -> requested.remove(category.getId()));
            throw new EntityNotFoundException("Categories " + requested + " not found");
        }
        return new ArrayList<>(found);
    }

    private Transaction findTransaction(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Transaction " + id + " not found"));
    }

    @Override
    public Page<TransactionDto> getTransactionsByDateRange(OffsetDateTime from, OffsetDateTime to, List<Long> userids, int page, int size) {
        Pageable pageRequest = PageRequest.of(page, size, NEWEST_FIRST);
        Page<Transaction> result = transactionRepository.findAllByDateFromBetweenAndUserAccountIdIn(from, to, userids, pageRequest);
        return result.map(transaction -> conversionService.convert(transaction, TransactionDto.class));
    }
}
