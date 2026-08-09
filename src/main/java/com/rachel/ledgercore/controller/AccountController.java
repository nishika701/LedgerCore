package com.rachel.ledgercore.controller;

import com.rachel.ledgercore.dto.*;
import com.rachel.ledgercore.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/accounts")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest createAccountRequest){
        AccountResponse response = accountService.createAccount(createAccountRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByAccountNumber(@Valid @PathVariable String accountNumber){
        AccountResponse response = accountService.getAccountByAccountNumber(accountNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAllAccounts(){
        List<AccountResponse> response = accountService.getAllAccounts();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/deposit/{accountNumber}")
    public ResponseEntity<DepositResponse> depositMoney(@Valid @RequestBody DepositRequest depositRequest){
        DepositResponse response = accountService.depositMoney(depositRequest);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/withdraw/{accountNumber}")
    public ResponseEntity<WithdrawResponse> withdrawMoney(@Valid @RequestBody WithdrawRequest withdrawRequest){
        WithdrawResponse response = accountService.withdrawMoney(withdrawRequest);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{accountNumber}")
    public ResponseEntity<DeleteAccountResponse> deleteAccount(@Valid @PathVariable String accountNumber){
        DeleteAccountResponse response = accountService.deleteAccount(accountNumber);
        return ResponseEntity.ok(response);
    }
}

/*
AUTH
POST   /auth/register
POST   /auth/login

ACCOUNT - done
POST   /accounts
GET    /accounts
GET    /accounts/{accountNumber}
PATCH  /accounts/{accountNumber}/deposit
PATCH  /accounts/{accountNumber}/withdraw
DELETE /accounts/{accountNumber}

TRANSFER
POST   /api/v1/transfer
GET    /api/v1/transfers/{transferId}
GET    /api/v1/transfers
GET    /api/v1/accounts/{accountNumber}/transfers

LEDGER
GET    /api/v1/accounts/{accountNumber}/ledger

NOTIFICATION
GET    /api/v1/notifications/{notificationId}
GET    /api/v1/accounts/{accountNumber}/notifications

HEALTH / OBSERVABILITY
GET    /actuator/health
GET    /actuator/metrics
GET    /actuator/prometheus
 */
