package com.rachel.ledgercore.repository;

import com.rachel.ledgercore.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, UUID> {
    List<Transfer> findByStatus(String status);
    Optional<Transfer> findByIdempotencyKey(String idempotencyKey);
}
