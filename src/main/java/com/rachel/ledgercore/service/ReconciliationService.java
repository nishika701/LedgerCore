package com.rachel.ledgercore.service;

import com.rachel.ledgercore.enums.ReconciliationStatus;
import com.rachel.ledgercore.exception.AccountNotFoundException;
import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.model.ReconciliationRecord;
import com.rachel.ledgercore.repository.AccountRepository;
import com.rachel.ledgercore.repository.LedgerEntryRepository;
import com.rachel.ledgercore.repository.ReconciliationRecordRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReconciliationService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final ReconciliationRecordRepository reconciliationRecordRepository;

    @Transactional
    public ReconciliationRecord reconcileAccount(UUID accountId){
        Account account = accountRepository.findById(accountId).orElseThrow(() -> new AccountNotFoundException("Account not found with ID: " + accountId));

        BigDecimal initialBalance = account.getInitialBalance() != null ? account.getInitialBalance() : BigDecimal.ZERO;
        BigDecimal totalCredits = Optional.ofNullable(ledgerEntryRepository.sumCreditsByAccountId(accountId)).orElse(BigDecimal.ZERO);
        BigDecimal totalDebits = Optional.ofNullable(ledgerEntryRepository.sumDebitsByAccountId(accountId)).orElse(BigDecimal.ZERO);

        BigDecimal calculatedBalance = initialBalance.add(totalCredits).subtract(totalDebits);
        BigDecimal recordedBalance = account.getBalance();
        BigDecimal discrepancy = recordedBalance.subtract(calculatedBalance);

        boolean isMatched = discrepancy.compareTo(BigDecimal.ZERO) == 0;
        ReconciliationStatus status = isMatched ? ReconciliationStatus.MATCHED : ReconciliationStatus.DISCREPANCY_DETECTED;
        String details = isMatched ?
        String.format("Audit passed for account %s. Recorded: %s, Calculated: %s (Credits: %s, Debits: %s)", account.getAccountNumber(), recordedBalance, calculatedBalance,totalCredits ,totalDebits)
        : String.format("CRITICAL DISCREPANCY on account %s! Recorded: %s, Calculated: %s (Delta: %s)", account.getAccountNumber(), recordedBalance, calculatedBalance, discrepancy);

        if(!isMatched){
            log.error(details);
        } else{
            log.error(details);
        }

        ReconciliationRecord record = ReconciliationRecord.builder()
                .account(account)
                .recordedBalance(recordedBalance)
                .calculatedBalance(calculatedBalance)
                .discrepancyAmount(discrepancy)
                .status(status)
                .reconciledAt(LocalDateTime.now())
                .details(details).build();

        return reconciliationRecordRepository.save(record);
    }

    @Transactional
    public List<ReconciliationRecord> reconcileAllAccounts() {
        log.info("Starting automated reconciliation audit across all accounts...");
        List<Account> accounts = accountRepository.findAll();
        List<ReconciliationRecord> records = new ArrayList<>();

        for (Account account : accounts){
            records.add(reconcileAccount(account.getId()));
        }

        long discrepancies = records.stream().filter(r -> r.getStatus() == ReconciliationStatus.DISCREPANCY_DETECTED).count();

        log.info("Reconciliation audit complete. Total audited: {}, Discrepancies found: {}",records.size(), discrepancies);
        return records;
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void runScheduleNightlyAudit() {
        reconcileAllAccounts();
    }
}
