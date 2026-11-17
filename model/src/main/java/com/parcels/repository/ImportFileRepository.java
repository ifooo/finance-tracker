package com.parcels.repository;

import com.parcels.domain.ImportFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ImportFileRepository extends JpaRepository<ImportFile, Long> {

    Optional<ImportFile> findBySha256(String sha256);
}
