package com.rachel.ledgercore.service;

import com.rachel.ledgercore.dto.*;
import com.rachel.ledgercore.enums.NotificationType;
import com.rachel.ledgercore.exception.AccountAlreadyExistingException;
import com.rachel.ledgercore.exception.AccountNotFoundException;
import com.rachel.ledgercore.exception.InsufficientBalanceException;
import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.repository.AccountRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.rachel.ledgercore.model.User;
import com.rachel.ledgercore.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

//    public AccountService(AccountRepository accountRepository,NotificationService notificationService){
//        this.accountRepository = accountRepository;
//        this.notificationService = notificationService;
//    }

    public AccountResponse createAccount(CreateAccountRequest createAccountRequest){
        String username = SecurityContextHolder.getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
        if(accountRepository.findByAccountNumber(createAccountRequest.getAccountNumber()).isPresent()){
            throw new AccountAlreadyExistingException("Account number already exists!");
        }
        Account account = Account.builder()
                .user(user)
                .currency(createAccountRequest.getCurrency())
                .accountNumber(createAccountRequest.getAccountNumber())
                .ownerName(createAccountRequest.getOwnerName())
                .balance(BigDecimal.ZERO).build();
        Account savedAccount = accountRepository.save(account);
        return AccountResponse.builder()
                .id(savedAccount.getId())
                .currency(savedAccount.getCurrency())
                .accountNumber(savedAccount.getAccountNumber())
                .ownerName(savedAccount.getOwnerName())
                .balance(savedAccount.getBalance())
                .message("Account created successfully!")
                .createdAt(savedAccount.getCreatedAt())
                .build();
    }

    public AccountResponse getAccountByAccountNumber(String accountNumber){
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new AccountNotFoundException("Account not found!"));
        return AccountResponse.builder()
                .id(account.getId())
                .createdAt(account.getCreatedAt())
                .currency(account.getCurrency())
                .accountNumber(account.getAccountNumber())
                .ownerName(account.getOwnerName())
                .balance(account.getBalance())
                .message("Account retrieved successfully!")
                .build();
    }

    public List<AccountResponse> getAllAccounts(){
        return accountRepository.findAll().stream()
                .map(account -> AccountResponse.builder()
                        .id(account.getId())
                        .createdAt(account.getCreatedAt())
                        .currency(account.getCurrency())
                        .accountNumber(account.getAccountNumber())
                        .ownerName(account.getOwnerName())
                        .balance(account.getBalance())
                        .message("Account retrieved successfully!")
                        .build())
                .toList();
    }


    @Transactional
    public DepositResponse depositMoney(String accountNumber,DepositRequest depositRequest){
        Account acc = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new AccountNotFoundException("Account not found!"));
        BigDecimal newBalance =  acc.getBalance().add(depositRequest.getAmount());
        acc.setBalance(newBalance);
        accountRepository.save(acc);
        notificationService.createNotification(acc,"Money deposited successfully!", NotificationType.MONEY_DEPOSITED);
        return DepositResponse.builder()
                .accountNumber(accountNumber)
                .balance(acc.getBalance())
                .message("Money deposited successfully!")
                .build();
    }

    @Transactional
    public WithdrawResponse withdrawMoney(String accountNumber,WithdrawRequest withdrawRequest){
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new AccountNotFoundException("Account not found!"));
        if (account.getBalance().compareTo(withdrawRequest.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in account!");
        }
        BigDecimal newBalance =  account.getBalance().subtract(withdrawRequest.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);
        notificationService.createNotification(account,"Money withdrawn successfully!", NotificationType.MONEY_WITHDRAWN);
        return WithdrawResponse.builder()
                .accountNumber(accountNumber)
                .balance(account.getBalance())
                .message("Money withdrawn successfully!")
                .build();
    }

    public DeleteAccountResponse deleteAccount(String accountNumber){
        Account account =  accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new AccountNotFoundException("Account not found!"));
        accountRepository.delete(account);
        return DeleteAccountResponse.builder()
                .message("Account deleted successfully!")
                .accountNumber(accountNumber)
                .deletedOn(LocalDateTime.now())
                .build();
    }

}
