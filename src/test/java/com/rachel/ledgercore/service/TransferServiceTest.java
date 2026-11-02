package com.rachel.ledgercore.service;

import com.rachel.ledgercore.dto.TransferRequest;
import com.rachel.ledgercore.exception.AccountNotFoundException;
import com.rachel.ledgercore.exception.InsufficientBalanceException;
import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.model.User;
import com.rachel.ledgercore.repository.AccountRepository;
import com.rachel.ledgercore.repository.TransferRepository;
import com.rachel.ledgercore.repository.LedgerEntryRepository;
import com.rachel.ledgercore.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransferServiceTest {
    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private TransferService transferService;

    @Test
    void shouldRejectTransferToSameAccount() {

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("ACC123")
                .toAccountNumber("ACC123")
                .amount(new BigDecimal("500.00"))
                .idempotencyKey("test-key-1")
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            transferService.transferMoney(request);
        });
    }

    @Test
    void shouldRejectInsufficientBalance(){

        User user = mock(User.class);
        when(user.getUsername()).thenReturn("testuser");

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("testuser", null));

        Account fromAccount = Account.builder()
                .accountNumber("ACC123")
                .balance(new BigDecimal("200.00"))
                .user(user)
                .build();

        Account toAccount = Account.builder()
                .accountNumber("ACC456")
                .balance(new BigDecimal("1000.0"))
                .build();

        when(accountRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(fromAccount));

        when(transferRepository.findByIdempotencyKey("test-key-2")).thenReturn(Optional.empty());

        when(accountRepository.findByAccountNumber("ACC456")).thenReturn(Optional.of(toAccount));

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("ACC123")
                .toAccountNumber("ACC456")
                .amount(new BigDecimal("500.00"))
                .idempotencyKey("test-key-2")
                .build();

        assertThrows(InsufficientBalanceException.class, () -> {
            transferService.transferMoney(request);
        });

        verify(transferRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).saveAll(anyList());
    }

    @Test
    void shouldThrowExceptionWhenSenderAccountDoesNotExist() {
        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("INVALID123")
                .toAccountNumber("ACC456")
                .amount(new BigDecimal("500.00"))
                .idempotencyKey("test-key-3")
                .build();

        when(accountRepository.findByAccountNumber("INVALID123")).thenReturn(Optional.empty());

        assertThrows(AccountNotFoundException.class, () -> {
            transferService.transferMoney(request);
        });

        verify(transferRepository, never()).findByIdempotencyKey(anyString());
    }

    @Test
    void shouldThrowExceptionWhenReceiverAccountDoesNotExist() {
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("testuser");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null)
        );

        Account fromAccount = Account.builder()
                .accountNumber("ACC123")
                .balance(new BigDecimal("1000.00"))
                .user(user)
                .build();

        when(accountRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(fromAccount));

        when(transferRepository.findByIdempotencyKey("test-key-4")).thenReturn(Optional.empty());

        when(accountRepository.findByAccountNumber("INVALID456")).thenReturn(Optional.empty());

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("ACC123")
                .toAccountNumber("INVALID456")
                .amount(new BigDecimal("500.00"))
                .idempotencyKey("test-key-4")
                .build();

        assertThrows(AccountNotFoundException.class, () -> {
            transferService.transferMoney(request);
        });
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
