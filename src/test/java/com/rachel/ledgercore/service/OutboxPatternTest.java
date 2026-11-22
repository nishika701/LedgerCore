package com.rachel.ledgercore.service;

import com.rachel.ledgercore.dto.TransferRequest;
import com.rachel.ledgercore.dto.TransferResponse;
import com.rachel.ledgercore.enums.Currency;
import com.rachel.ledgercore.enums.OutboxStatus;
import com.rachel.ledgercore.enums.Role;
import com.rachel.ledgercore.enums.Status;
import com.rachel.ledgercore.gateway.MockPaymentGatewayClient;
import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.model.OutboxEvent;
import com.rachel.ledgercore.model.User;
import com.rachel.ledgercore.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class OutboxPatternTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private OutboxRelayService outboxRelayService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

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
    private MockPaymentGatewayClient mockPaymentGatewayClient;

    @BeforeEach
    void setUp() {
        mockPaymentGatewayClient.setSimulateFailure(false);
        mockPaymentGatewayClient.setSimulateTimeout(false);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        outboxEventRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        notificationRepository.deleteAll();
        transferRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldPersistOutboxEventAtomicallyWhenTransferCompletes() {

        User user = userRepository.save(User.builder()
                .username("outbox-user-" + UUID.randomUUID())
                .password("password")
                .role(Role.USER)
                .build());

        Account sender = accountRepository.save(Account.builder()
                .accountNumber("OUTBOX-SENDER-" + UUID.randomUUID())
                .ownerName("Sender")
                .currency(Currency.INR)
                .balance(new BigDecimal("1000.00"))
                .user(user)
                .build());

        Account receiver = accountRepository.save(Account.builder()
                .accountNumber("OUTBOX-RECEIVER-" + UUID.randomUUID())
                .ownerName("Receiver")
                .currency(Currency.INR)
                .balance(BigDecimal.ZERO)
                .user(user)
                .build());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), null)
        );

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber(sender.getAccountNumber())
                .toAccountNumber(receiver.getAccountNumber())
                .amount(new BigDecimal("250.00"))
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        TransferResponse response = transferService.transferMoney(request);
        assertEquals(Status.COMPLETED, response.getStatus());

        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertEquals(1, events.size(), "Expected exactly 1 outbox event to be created");
        OutboxEvent event = events.get(0);
        assertEquals("TRANSFER", event.getAggregateType());
        assertEquals(response.getId(), event.getAggregateId());
        assertEquals("TRANSFER_COMPLETED", event.getEventType());
        assertTrue(event.getPayload().contains("250.00"));
        assertNotNull(event.getCreatedAt());

        outboxRelayService.processOutboxEvents();

        OutboxEvent processedEvent = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertEquals(OutboxStatus.PROCESSED, processedEvent.getStatus());
        assertNotNull(processedEvent.getProcessedAt(), "ProcessedAt timestamp must be populated");

    }

    @Test
    void shouldNotPersistOutboxEventWhenTransferFails(){
        User user = userRepository.save(User.builder()
                .username("fail-user-" + UUID.randomUUID())
                .password("password")
                .role(Role.USER)
                .build());

        Account sender = accountRepository.save(Account.builder()
                .accountNumber("FAIL-SENDER-" + UUID.randomUUID())
                .ownerName("Sender")
                .currency(Currency.INR)
                .balance(new BigDecimal("1000.00"))
                .user(user)
                .build());

        Account receiver = accountRepository.save(Account.builder()
                .accountNumber("FAIL-RECEIVER-" + UUID.randomUUID())
                .ownerName("Receiver")
                .currency(Currency.INR)
                .balance(BigDecimal.ZERO)
                .user(user)
                .build());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), null)
        );

        mockPaymentGatewayClient.setSimulateFailure(true);

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber(sender.getAccountNumber())
                .toAccountNumber(receiver.getAccountNumber())
                .amount(new BigDecimal("100.00"))
                .idempotencyKey(UUID.randomUUID().toString())
                .build();

        TransferResponse response = transferService.transferMoney(request);
        assertEquals(Status.FAILED, response.getStatus());


        List<OutboxEvent> events = outboxEventRepository.findAll();
        assertEquals(0, events.size(), "No outbox events should be created for failed transfers");
    }
}
