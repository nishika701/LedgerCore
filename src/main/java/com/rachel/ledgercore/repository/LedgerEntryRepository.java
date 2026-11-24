package com.rachel.ledgercore.repository;

import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.model.LedgerEntry;
import com.rachel.ledgercore.model.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {
    List<LedgerEntry> findByTransfer(Transfer transfer);
    List<LedgerEntry> findByAccount(Account account);

    @Query("SELECT SUM(e.amount) FROM LedgerEntry e WHERE e.account.id = :accountId AND e.entryType = com.rachel.ledgercore.enums.EntryType.CREDIT")
    BigDecimal sumCreditsByAccountId(@Param("accountId") UUID accountId);

    @Query("SELECT SUM(e.amount) FROM LedgerEntry e WHERE e.account.id = :accountId AND e.entryType = com.rachel.ledgercore.enums.EntryType.DEBIT")
    BigDecimal sumDebitsByAccountId(@Param("accountId") UUID accountId);
}
