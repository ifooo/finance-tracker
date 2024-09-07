package com.parcels.repository;

import com.parcels.domain.Transaction;
import com.parcels.domain.enums.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {


    Page<Transaction> findAllByTransactionType(TransactionType transactionType,
                                                                 Pageable pageable);

    Page<Transaction> findAllByDateFromBetweenAndUserAccountIdIn(OffsetDateTime from,
                                                                 OffsetDateTime to,
                                                                 List<Long> userIds,
                                                                 Pageable pageable
    );


}
