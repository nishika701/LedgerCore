package com.rachel.ledgercore.service;

import com.rachel.ledgercore.dto.TransferRequest;
import com.rachel.ledgercore.dto.TransferResponse;
import com.rachel.ledgercore.enums.Currency;
import com.rachel.ledgercore.enums.ReconciliationStatus;
import com.rachel.ledgercore.enums.Role;
import com.rachel.ledgercore.enums.Status;
import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.model.ReconciliationRecord;
import com.rachel.ledgercore.model.User;
import com.rachel.ledgercore.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class ReconciliationTest {

    @Autowired
    private ReconciliationService reconciliationService;

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private ReconciliationRecordRepository reconciliationRecordRepository;

    @AfterEach
    void tearDown(){
        SecurityContextHolder.clearContext();
        reconciliationRecordRepository.deleteAll();
        outboxEventRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        notificationRepository.deleteAll();
        transferRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldPassReconciliationWhenLedgerEntriesMatchBalance() {

        User user = userRepository.save(User.builder()
                .username("audit-user-" + UUID.randomUUID())
                .password("password")
                .role(Role.USER)
                .build());

        Account sender = accountRepository.save(Account.builder()
                .accountNumber("AUDIT-SENDER-" + UUID.randomUUID())
                .ownerName("Sender")
                .currency(Currency.INR)
                .balance(new BigDecimal("1000.00"))
                .initialBalance(new BigDecimal("1000.00"))
                .user(user)
                .build());

        Account receiver = accountRepository.save(Account.builder()
                .accountNumber("AUDIT-RECEIVER-" + UUID.randomUUID())
                .ownerName("Receiver")
                .currency(Currency.INR)
                .balance(BigDecimal.ZERO)
                .initialBalance(BigDecimal.ZERO)
                .user(user)
                .build());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), null)
        );

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber(sender.getAccountNumber())
                .toAccountNumber(receiver.getAccountNumber())
                .amount(new BigDecimal("300.0"))
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        TransferResponse response = transferService.transferMoney(request);
        assertEquals(Status.COMPLETED, response.getStatus());

        ReconciliationRecord senderRecord = reconciliationService.reconcileAccount(sender.getId());
        assertEquals(ReconciliationStatus.MATCHED, senderRecord.getStatus());
        assertEquals(0, senderRecord.getDiscrepancyAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, senderRecord.getCalculatedBalance().compareTo(new BigDecimal("700.00")));
        assertEquals(0, senderRecord.getRecordedBalance().compareTo(new BigDecimal("700.00")));

        ReconciliationRecord receiverRecord = reconciliationService.reconcileAccount(receiver.getId());
        assertEquals(ReconciliationStatus.MATCHED, receiverRecord.getStatus());
        assertEquals(0, receiverRecord.getDiscrepancyAmount().compareTo(BigDecimal.ZERO));
        assertEquals(0, receiverRecord.getCalculatedBalance().compareTo(new BigDecimal("300.00")));
        assertEquals(0, receiverRecord.getRecordedBalance().compareTo(new BigDecimal("300.00")));

    }

    @Test
    void shouldDetectDiscrepancyWhenAccountBalanceIsTampered() {

        User user = userRepository.save(User.builder()
                .username("tamper-user-" + UUID.randomUUID())
                .password("password")
                .role(Role.USER)
                .build());

        Account account = accountRepository.save(Account.builder()
                .accountNumber("TAMPER-ACC-" + UUID.randomUUID())
                .ownerName("Victim")
                .currency(Currency.INR)
                .balance(new BigDecimal("500.00"))
                .initialBalance(new BigDecimal("500.00"))
                .user(user)
                .build());

        account.setBalance(new BigDecimal("700.00"));
        accountRepository.save(account);

        ReconciliationRecord record = reconciliationService.reconcileAccount(account.getId());

        assertEquals(ReconciliationStatus.DISCREPANCY_DETECTED, record.getStatus());
        assertEquals(0, record.getDiscrepancyAmount().compareTo(new BigDecimal("200.00")));
        assertEquals(0, record.getRecordedBalance().compareTo(new BigDecimal("700.00")));
        assertEquals(0, record.getCalculatedBalance().compareTo(new BigDecimal("500.00")));
        assertTrue(record.getDetails().contains("CRITICAL DISCREPANCY"));
    }

    @Test
    void shouldReconcileAllAccountInSystem() {
        User user = userRepository.save(User.builder()
                .username("batch-user-" + UUID.randomUUID())
                .password("password")
                .role(Role.USER)
                .build());
        accountRepository.save(Account.builder()
                .accountNumber("BATCH-1-" + UUID.randomUUID())
                .ownerName("User 1")
                .currency(Currency.INR)
                .balance(new BigDecimal("100.00"))
                .initialBalance(new BigDecimal("100.00"))
                .user(user)
                .build());
        accountRepository.save(Account.builder()
                .accountNumber("BATCH-2-" + UUID.randomUUID())
                .ownerName("User 2")
                .currency(Currency.INR)
                .balance(new BigDecimal("200.00"))
                .initialBalance(new BigDecimal("200.00"))
                .user(user)
                .build());

        List<ReconciliationRecord> records = reconciliationService.reconcileAllAccounts();
        assertTrue(records.size() >= 2);
        assertTrue(records.stream().allMatch(r -> r.getStatus() == ReconciliationStatus.MATCHED));
    }
}
