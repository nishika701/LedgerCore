package com.rachel.ledgercore.service;

import com.rachel.ledgercore.dto.TransferRequest;
import com.rachel.ledgercore.dto.TransferResponse;
import com.rachel.ledgercore.enums.Currency;
import com.rachel.ledgercore.enums.Role;
import com.rachel.ledgercore.enums.Status;
import com.rachel.ledgercore.gateway.MockPaymentGatewayClient;
import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.model.Transfer;
import com.rachel.ledgercore.model.User;
import com.rachel.ledgercore.repository.*;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class PaymentResilienceTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private MockPaymentGatewayClient mockPaymentGatewayClient;

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
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp(){
        mockPaymentGatewayClient.setSimulateFailure(false);
        mockPaymentGatewayClient.setSimulateTimeout(false);

        circuitBreakerRegistry.circuitBreaker("paymentGateway").reset();
    }

    @AfterEach
    void tearDown(){
        mockPaymentGatewayClient.setSimulateFailure(false);
        mockPaymentGatewayClient.setSimulateTimeout(false);
        SecurityContextHolder.clearContext();
        ledgerEntryRepository.deleteAll();
        notificationRepository.deleteAll();
        transferRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldFailGracefullyAndPreserveBalancesWhenGatewayFails() {
        User user = userRepository.save(User.builder()
                .username("resilience-user-" + UUID.randomUUID())
                .password("pwd")
                .role(Role.USER)
                .build());

        Account sender = accountRepository.save(Account.builder()
                .accountNumber("RES-SENDER-" + UUID.randomUUID())
                .ownerName("Sender")
                .currency(Currency.INR)
                .balance(new BigDecimal("1000.00"))
                .user(user)
                .build());
        Account receiver = accountRepository.save(Account.builder()
                .accountNumber("RES-RECEIVER-" + UUID.randomUUID())
                .ownerName("Receiver")
                .currency(Currency.INR)
                .balance(BigDecimal.ZERO)
                .user(user)
                .build());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(),null)
        );

        mockPaymentGatewayClient.setSimulateFailure(true);

        String idempotencyKey = UUID.randomUUID().toString();

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber(sender.getAccountNumber())
                .toAccountNumber(receiver.getAccountNumber())
                .amount(new BigDecimal("300.00"))
                .idempotencyKey(idempotencyKey)
                .build();

        TransferResponse response = transferService.transferMoney(request);

        assertEquals(Status.FAILED, response.getStatus());

        Account updatedSender = accountRepository.findByAccountNumber(sender.getAccountNumber()).orElseThrow();
        assertEquals(0, new BigDecimal("1000.00").compareTo(updatedSender.getBalance()),
                "Sender balance was deducted despite gateway failure!");

        Account updatedReceiver = accountRepository.findByAccountNumber(receiver.getAccountNumber()).orElseThrow();
        assertEquals(0, BigDecimal.ZERO.compareTo(updatedReceiver.getBalance()),
                "Receiver balance was credited despite gateway failure!");

        Transfer savedTransfer = transferRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();
        assertEquals(Status.FAILED, savedTransfer.getStatus());

        var entries = ledgerEntryRepository.findByTransfer(savedTransfer);
        assertEquals(0, entries.size(), "Ledger entries were written for a failed transfer!");
    }
    @Test
    void shouldTripCircuitBreakerToOpenWhenFailureRateExceedsThreshold() {
        User user = userRepository.save(User.builder()
                .username("cb-user-" + UUID.randomUUID())
                .password("password")
                .role(Role.USER)
                .build());

        Account sender = accountRepository.save(Account.builder()
                .accountNumber("CB-SENDER-" + UUID.randomUUID())
                .ownerName("Sender")
                .currency(Currency.INR)
                .balance(new BigDecimal("5000.00"))
                .user(user)
                .build());

        Account receiver = accountRepository.save(Account.builder()
                .accountNumber("CB-RECEIVER-" + UUID.randomUUID())
                .ownerName("Receiver")
                .currency(Currency.INR)
                .balance(BigDecimal.ZERO)
                .user(user)
                .build());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUsername(), null)
        );

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("paymentGateway");
        assertEquals(CircuitBreaker.State.CLOSED, cb.getState(), "Circuit breaker should start in CLOSED state");

        mockPaymentGatewayClient.setSimulateFailure(true);

        for (int i = 0; i < 10; i++) {
            TransferRequest request = TransferRequest.builder()
                    .fromAccountNumber(sender.getAccountNumber())
                    .toAccountNumber(receiver.getAccountNumber())
                    .amount(new BigDecimal("10.00"))
                    .idempotencyKey(UUID.randomUUID().toString())
                    .build();
            transferService.transferMoney(request);
        }

        System.out.println("====== CIRCUIT BREAKER STATE: " + cb.getState() + " ======");
        assertEquals(CircuitBreaker.State.OPEN, cb.getState(),
                "Circuit breaker failed to trip to OPEN after 100% failure rate!");
    }
}
