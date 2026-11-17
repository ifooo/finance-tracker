package com.parcels.repository;

import com.parcels.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Page<Transaction> findAllByDateFromBetweenAndUserAccountIdIn(OffsetDateTime from,
                                                                 OffsetDateTime to,
                                                                 List<Long> userIds,
                                                                 Pageable pageable
    );

    /** All transactions with {@code from <= dateFrom < to}, oldest first, with categories and person loaded (unpaged, for reports). */
    @EntityGraph(attributePaths = {"categories", "userAccount"})
    @Query("select distinct t from Transaction t where t.dateFrom >= :from and t.dateFrom < :to order by t.dateFrom, t.id")
    List<Transaction> findAllInRange(OffsetDateTime from, OffsetDateTime to);

    /** Every transaction, oldest first, with categories and person loaded (for the CSV export). */
    @EntityGraph(attributePaths = {"categories", "userAccount"})
    @Query("select distinct t from Transaction t order by t.dateFrom, t.id")
    List<Transaction> findAllForExport();

    boolean existsByExternalId(String externalId);
}
