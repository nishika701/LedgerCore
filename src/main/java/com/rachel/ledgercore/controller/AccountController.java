package com.rachel.ledgercore.controller;

import com.rachel.ledgercore.dto.CreateAccountRequest;
import com.rachel.ledgercore.dto.CreateAccountResponse;
import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<CreateAccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest createAccountRequest){
        CreateAccountResponse response = accountService.createAccount(createAccountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<CreateAccountResponse> getAccountByAccountNumber(@Valid @PathVariable String accountNumber){
        CreateAccountResponse response = accountService.getAccountByAccountNumber(accountNumber);
        return ResponseEntity.ok(response);
    }
}


