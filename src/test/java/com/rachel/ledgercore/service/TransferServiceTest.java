package com.rachel.ledgercore.service;

import com.rachel.ledgercore.dto.TransferRequest;
import com.rachel.ledgercore.dto.TransferResponse;
import com.rachel.ledgercore.enums.Status;
import com.rachel.ledgercore.exception.AccountNotFoundException;
import com.rachel.ledgercore.exception.InsufficientBalanceException;
import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.model.Transfer;
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
import org.springframework.security.access.AccessDeniedException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void shouldReturnExistingTransferForSameIdempotencyKey(){

        User user = mock(User.class);
        when(user.getUsername()).thenReturn("testuser");

        Account fromAccount = Account.builder()
                .accountNumber("ACC123")
                .user(user)
                .build();

        Account toAccount = Account.builder()
                .accountNumber("ACC456")
                .build();

        Transfer existingTransfer = Transfer.builder()
                .id(UUID.randomUUID())
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(new BigDecimal("500"))
                .status(Status.COMPLETED)
                .idempotencyKey("test-key-5")
                .build();

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("testuser",null));

        when(accountRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(fromAccount));

        when(transferRepository.findByIdempotencyKey("test-key-5")).thenReturn(Optional.of(existingTransfer));

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("ACC123")
                .toAccountNumber("ACC456")
                .amount(new BigDecimal("500"))
                .idempotencyKey("test-key-5")
                .build();

        TransferResponse response = transferService.transferMoney(request);

        assertEquals(existingTransfer.getId(),response.getId());
        assertEquals(Status.COMPLETED, response.getStatus());
        assertEquals("Transfer already processed", response.getMessage());

        verify(accountRepository, never()).findByAccountNumber("ACC456");

        verify(transferRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).saveAll(anyList());
        verify(notificationService, never()).createNotification(
                any(), anyString(), any()
        );
    }

    @Test
    void shouldRejectSameIdempotencyForDifferentRequest() {
        User user = mock(User.class);
        when(user.getUsername()).thenReturn("testuser");

        Account fromAccount = Account.builder()
                .accountNumber("ACC123")
                .user(user)
                .build();

        Account toAccount = Account.builder()
                .accountNumber("ACC456")
                .build();

        Transfer existingTransfer = Transfer.builder()
                .id(UUID.randomUUID())
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(new BigDecimal("500"))
                .status(Status.COMPLETED)
                .idempotencyKey("test-key-6")
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser",null)
        );

        when(accountRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(fromAccount));
        when(transferRepository.findByIdempotencyKey("test-key-6")).thenReturn(Optional.of(existingTransfer));

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("ACC123")
                .toAccountNumber("ACC456")
                .amount(new BigDecimal("700"))
                .idempotencyKey("test-key-6")
                .build();

        assertThrows(IllegalArgumentException.class, () -> transferService.transferMoney(request));

        verify(transferRepository, never()).save(any());
        verify(ledgerEntryRepository, never()).saveAll(anyList());

        verify(accountRepository, never()).findByAccountNumber("ACC456");

        verify(notificationService, never()).createNotification(any(), anyString(), any());
    }

    @Test
    void shouldRejectTransferFromAccountOwnedByAnotherUser() {

        // Arrange
        User accountOwner = mock(User.class);
        when(accountOwner.getUsername()).thenReturn("anotheruser");

        Account fromAccount = Account.builder()
                .accountNumber("ACC123")
                .user(accountOwner)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null)
        );

        when(accountRepository.findByAccountNumber("ACC123"))
                .thenReturn(Optional.of(fromAccount));

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("ACC123")
                .toAccountNumber("ACC456")
                .amount(new BigDecimal("500"))
                .idempotencyKey("test-key-7")
                .build();

        assertThrows(AccessDeniedException.class,
                () -> transferService.transferMoney(request));

        verify(transferRepository, never())
                .findByIdempotencyKey(anyString());

        verify(transferRepository, never()).save(any());

        verify(ledgerEntryRepository, never()).saveAll(anyList());

        verify(notificationService, never()).createNotification(
                any(), anyString(), any()
        );
    }

    @Test
    void shouldSuccessfullyTransferMoney() {

        User user = mock(User.class);
        when(user.getUsername()).thenReturn("testuser");

        Account fromAccount = Account.builder()
                .accountNumber("ACC123")
                .balance(new BigDecimal("1000"))
                .user(user)
                .build();

        Account toAccount = Account.builder()
                .accountNumber("ACC456")
                .balance(new BigDecimal("500"))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("testuser", null)
        );

        when(accountRepository.findByAccountNumber("ACC123"))
                .thenReturn(Optional.of(fromAccount));

        when(accountRepository.findByAccountNumber("ACC456"))
                .thenReturn(Optional.of(toAccount));

        when(transferRepository.findByIdempotencyKey("test-key-8"))
                .thenReturn(Optional.empty());

        TransferRequest request = TransferRequest.builder()
                .fromAccountNumber("ACC123")
                .toAccountNumber("ACC456")
                .amount(new BigDecimal("300"))
                .idempotencyKey("test-key-8")
                .build();

        TransferResponse response = transferService.transferMoney(request);
        
        assertEquals(0, new BigDecimal("700")
                .compareTo(fromAccount.getBalance()));

        assertEquals(0, new BigDecimal("800")
                .compareTo(toAccount.getBalance()));

        assertEquals(Status.COMPLETED, response.getStatus());
        assertEquals(new BigDecimal("300").compareTo(response.getAmount()), 0);
        assertEquals("Transfer successful", response.getMessage());

        verify(transferRepository, times(1)).save(any(Transfer.class));
        verify(ledgerEntryRepository, times(1)).saveAll(anyList());

        verify(notificationService, times(2)).createNotification(
                any(Account.class), anyString(), any()
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
