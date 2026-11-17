package com.parcels.repository;

import com.parcels.domain.ReportRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRunRepository extends JpaRepository<ReportRun, Long> {

    boolean existsByMonth(String month);
}
