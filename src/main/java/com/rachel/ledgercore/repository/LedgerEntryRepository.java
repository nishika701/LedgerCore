package com.rachel.ledgercore.repository;

import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.model.LedgerEntry;
import com.rachel.ledgercore.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByTransfer(Transfer transfer);
    List<LedgerEntry> findByAccount(Account account);
}
