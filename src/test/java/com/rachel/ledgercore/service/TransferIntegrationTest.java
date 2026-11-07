package com.rachel.ledgercore.service;

import com.rachel.ledgercore.dto.TransferRequest;
import com.rachel.ledgercore.dto.TransferResponse;
import com.rachel.ledgercore.enums.Currency;
import com.rachel.ledgercore.enums.EntryType;
import com.rachel.ledgercore.enums.Role;
import com.rachel.ledgercore.enums.Status;
import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.model.LedgerEntry;
import com.rachel.ledgercore.model.Transfer;
import com.rachel.ledgercore.model.User;
import com.rachel.ledgercore.repository.AccountRepository;
import com.rachel.ledgercore.repository.LedgerEntryRepository;
import com.rachel.ledgercore.repository.TransferRepository;
import com.rachel.ledgercore.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
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

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class TransferIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldSuccessfullyTransferMoneyAndPersistRecords() {

        User user = User.builder()
                .username("integration-" + UUID.randomUUID())
                .password("test-password")
                .role(Role.USER)
                .build();

        user = userRepository.save(user);

        Account fromAccount = Account.builder()
                .accountNumber("SENDER-" + UUID.randomUUID())
                .ownerName("Sender")
                .currency(Currency.INR)
                .user(user)
                .balance(new BigDecimal("1000.0"))
                .build();

        fromAccount = accountRepository.save(fromAccount);

        Account toAccount = Account.builder()
                .currency(Currency.INR)
                .accountNumber("RECEIVER-" + UUID.randomUUID())
                .ownerName("Receiver")
                .user(user)
                .balance(new BigDecimal("500.0"))
                .build();

        toAccount = accountRepository.save(toAccount);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        user.getUsername(), null
                )
        );

        String idempotencyKey = UUID.randomUUID().toString();

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber(fromAccount.getAccountNumber())
                .toAccountNumber(toAccount.getAccountNumber())
                .amount(new BigDecimal("300.0"))
                .idempotencyKey(idempotencyKey)
                .build();

        TransferResponse response = transferService.transferMoney(request);

        entityManager.flush();
        entityManager.clear();

        Account updatedSender = accountRepository.findByAccountNumber(fromAccount.getAccountNumber()).orElseThrow();
        Account updatedReceiver = accountRepository.findByAccountNumber(toAccount.getAccountNumber()).orElseThrow();

        assertEquals(0,
                new BigDecimal("700").compareTo(updatedSender.getBalance()));

        assertEquals(0,
                new BigDecimal("800.0").compareTo(updatedReceiver.getBalance()));

        Transfer savedTransfer = transferRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();

        assertEquals(Status.COMPLETED, savedTransfer.getStatus());
        assertEquals(0, new BigDecimal("300.0").compareTo(savedTransfer.getAmount()));

        assertEquals(savedTransfer.getId(), response.getId());

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransfer(savedTransfer);

        assertEquals(2, entries.size());

        LedgerEntry debit = entries.stream()
                .filter(entry -> entry.getEntryType() == EntryType.DEBIT)
                .findFirst()
                .orElseThrow();

        LedgerEntry credit = entries.stream()
                .filter(entry -> entry.getEntryType() == EntryType.CREDIT)
                .findFirst()
                .orElseThrow();

        assertEquals(0,
                new BigDecimal("300.00").compareTo(debit.getAmount()));

        assertEquals(0,
                new BigDecimal("300.00").compareTo(credit.getAmount()));

        assertEquals(fromAccount.getId(), debit.getAccount().getId());
        assertEquals(toAccount.getId(), credit.getAccount().getId());
    }
}
