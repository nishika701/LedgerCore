package com.rachel.ledgercore.repository;

import com.rachel.ledgercore.enums.ReconciliationStatus;
import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.model.ReconciliationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReconciliationRecordRepository extends JpaRepository<ReconciliationRecord, UUID> {
    List<ReconciliationRecord> findByAccountOrderByReconciledAtDesc(Account account);
    List<ReconciliationRecord> findByStatus(ReconciliationStatus status);
}
