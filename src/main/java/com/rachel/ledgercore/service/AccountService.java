package com.rachel.ledgercore.service;

import com.rachel.ledgercore.dto.CreateAccountRequest;
import com.rachel.ledgercore.dto.CreateAccountResponse;
import com.rachel.ledgercore.exception.AccountAlreadyExistingException;
import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public CreateAccountResponse createAccount(CreateAccountRequest createAccountRequest){
        if(accountRepository.findByAccountNumber(createAccountRequest.getAccountNumber()).isPresent()){
            throw new AccountAlreadyExistingException("Account number already exists!");
        }
        Account account = Account.builder()
                .currency(createAccountRequest.getCurrency())
                .accountNumber(createAccountRequest.getAccountNumber())
                .ownerName(createAccountRequest.getOwnerName())
                .balance(BigDecimal.ZERO).build();
        Account savedAccount = accountRepository.save(account);
        return CreateAccountResponse.builder()
                .id(savedAccount.getId())
                .currency(savedAccount.getCurrency())
                .accountNumber(savedAccount.getAccountNumber())
                .ownerName(savedAccount.getOwnerName())
                .balance(savedAccount.getBalance())
                .message("Account created successfully!")
                .createdAt(savedAccount.getCreatedAt())
                .build();
    }

}
