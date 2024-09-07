package com.parcels.transaction;

import com.parcels.domain.Category;
import com.parcels.domain.Transaction;
import com.parcels.domain.enums.TransactionType;
import com.parcels.mail.EmailService;
import com.parcels.repository.CategoryRepository;
import com.parcels.repository.TransactionRepository;
import com.parcels.repository.UserAccountRepository;
import com.parcels.transaction.dto.TransactionDto;
import com.parcels.transaction.dto.TransactionUpdateCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;
    private final ConversionService conversionService;
    private final UserAccountRepository userAccountRepository;
    private final EmailService emailService;

    @Override
    public TransactionDto getById(Long id) {
        return conversionService.convert(transactionRepository.findById(id).orElseThrow(), TransactionDto.class);
    }

    @Override
    public TransactionDto save(TransactionUpdateCommand transactionPersistCommand) {
        Transaction transaction = new Transaction();
        transaction.setTransactionType(transactionPersistCommand.type());
        if (transactionPersistCommand.categoryId() != null) {
            Category category = categoryRepository.findById(transactionPersistCommand.categoryId()).orElseThrow();
            transaction.setCategory(category);
        }
        transaction.setAmount(transactionPersistCommand.amount());
        transaction.setDescription(transactionPersistCommand.description());
        transaction.setDateFrom(transactionPersistCommand.datetime());
        transaction.setCurrency(transactionPersistCommand.currency());
        transaction.setUserAccount(userAccountRepository.findById(1L).orElseThrow());

        Transaction saved = transactionRepository.save(transaction);

        TransactionDto result = conversionService.convert(saved, TransactionDto.class);
        return result;
    }

    @Override
    public Boolean delete(Long id) {
        Transaction transaction = transactionRepository.findById(id).orElseThrow();
        transactionRepository.delete(transaction);
        return true;
    }

    @Override
    public Page<TransactionDto> getTransactionsByTypeAndDateRange(TransactionType transactionType, OffsetDateTime from, OffsetDateTime to, int page, int size) {
        Pageable pageRequest = PageRequest.of(page, size);
        Page<Transaction> result = transactionRepository.findAllByTransactionType(transactionType,   pageRequest);
        return result.map(transaction -> conversionService.convert(transaction, TransactionDto.class));
    }

    @Override
    public Page<TransactionDto> getTransactionsByDateRange(OffsetDateTime from, OffsetDateTime to, List<Long> userids, int page, int size) {
        Pageable pageRequest = PageRequest.of(page, size);
        Page<Transaction> result = transactionRepository.findAllByDateFromBetweenAndUserAccountIdIn(from, to, userids, pageRequest);
        return result.map(transaction -> conversionService.convert(transaction, TransactionDto.class));
    }
}
