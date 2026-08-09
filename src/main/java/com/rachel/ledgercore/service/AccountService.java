package com.rachel.ledgercore.service;

import com.rachel.ledgercore.dto.*;
import com.rachel.ledgercore.exception.AccountAlreadyExistingException;
import com.rachel.ledgercore.exception.AccountNotFoundException;
import com.rachel.ledgercore.exception.InsufficientBalanceException;
import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountResponse createAccount(CreateAccountRequest createAccountRequest){
        if(accountRepository.findByAccountNumber(createAccountRequest.getAccountNumber()).isPresent()){
            throw new AccountAlreadyExistingException("Account number already exists!");
        }
        Account account = Account.builder()
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


    public DepositResponse depositMoney(DepositRequest depositRequest){
        Account account = accountRepository.findByAccountNumber(depositRequest.getAccountNumber()).orElseThrow(() -> new AccountNotFoundException("Account not found!"));
        BigDecimal newBalance =  account.getBalance().add(depositRequest.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);
        return DepositResponse.builder()
                .accountNumber(depositRequest.getAccountNumber())
                .balance(account.getBalance())
                .message("Money deposited successfully!")
                .build();
    }

    public WithdrawResponse withdrawMoney(WithdrawRequest withdrawRequest){
        Account account = accountRepository.findByAccountNumber(withdrawRequest.getAccountNumber()).orElseThrow(() -> new AccountNotFoundException("Account not found!"));
        if (account.getBalance().compareTo(withdrawRequest.getAmount()) < 0) {
            throw new InsufficientBalanceException("Insufficient balance in account!");
        }
        BigDecimal newBalance =  account.getBalance().subtract(withdrawRequest.getAmount());
        account.setBalance(newBalance);
        accountRepository.save(account);
        return WithdrawResponse.builder()
                .accountNumber(withdrawRequest.getAccountNumber())
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
