package com.rachel.ledgercore.service;

import com.rachel.ledgercore.dto.TransferRequest;
import com.rachel.ledgercore.dto.TransferResponse;
import com.rachel.ledgercore.enums.EntryType;
import com.rachel.ledgercore.enums.Status;
import com.rachel.ledgercore.exception.AccountNotFoundException;
import com.rachel.ledgercore.exception.InsufficientBalanceException;
import com.rachel.ledgercore.exception.TransferNotFoundException;
import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.model.LedgerEntry;
import com.rachel.ledgercore.model.Transfer;
import com.rachel.ledgercore.repository.AccountRepository;
import com.rachel.ledgercore.repository.LedgerEntryRepository;
import com.rachel.ledgercore.repository.TransferRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor// instead of constructor injection, this is used
public class TransferService {
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransferRepository transferRepository;

    @Transactional
    public TransferResponse transferMoney(TransferRequest transferRequest){
        Transfer existing = transferRepository.findByIdempotencyKey(transferRequest.getIdempotencyKey()).orElse(null);
        if (existing != null) {
            return TransferResponse.builder()
                    .id(existing.getId())
                    .status(existing.getStatus())
                    .amount(existing.getAmount())
                    .message("Transfer already processed")
                    .build();
        }
        if (transferRequest.getFromAccountNumber()
                .equals(transferRequest.getToAccountNumber())) {

            throw new IllegalArgumentException(
                    "Sender and receiver cannot be the same account"
            );
        }
        Account fromAccount = accountRepository.findByAccountNumber(transferRequest.getFromAccountNumber()).orElseThrow(() -> new AccountNotFoundException("From account not found"));
        Account toAccount = accountRepository.findByAccountNumber(transferRequest.getToAccountNumber()).orElseThrow(() -> new AccountNotFoundException("To account not found"));
        if(fromAccount.getBalance().compareTo(transferRequest.getAmount()) < 0){
            throw new InsufficientBalanceException("Insufficient balance in from account");
        }
        fromAccount.setBalance(fromAccount.getBalance().subtract(transferRequest.getAmount()));
        toAccount.setBalance(toAccount.getBalance().add(transferRequest.getAmount()));

        Transfer transfer = Transfer.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .amount(transferRequest.getAmount())
                .status(Status.COMPLETED)
                .idempotencyKey(transferRequest.getIdempotencyKey())
                .build();
        transferRepository.save(transfer);

        LedgerEntry debit = LedgerEntry.builder()
                .account(fromAccount)
                .description("Debit for transfer")
                .amount(transferRequest.getAmount())
                .entryType(EntryType.DEBIT)
                .transfer(transfer)
                .build();

        LedgerEntry credit = LedgerEntry.builder()
                .account(toAccount)
                .description("Credit for transfer")
                .amount(transferRequest.getAmount())
                .entryType(EntryType.CREDIT)
                .transfer(transfer)
                .build();

        ledgerEntryRepository.saveAll(List.of(debit, credit));
        return TransferResponse.builder()
                .id(transfer.getId())
                .status(transfer.getStatus())
                .amount(transfer.getAmount())
                .fromAccountNumber(transfer.getFromAccount().getAccountNumber())
                .toAccountNumber(transfer.getToAccount().getAccountNumber())
                .message("Transfer successful")
                .build();
    }

    public TransferResponse getTransferByTransferId(UUID transferId){
        Transfer transfer = transferRepository.findById(transferId).orElseThrow(() -> new TransferNotFoundException("Transfer not found with id: " + transferId));
        return TransferResponse.builder()
                .id(transferId)
                .status(transfer.getStatus())
                .amount(transfer.getAmount())
                .fromAccountNumber(transfer.getFromAccount().getAccountNumber())
                .toAccountNumber(transfer.getToAccount().getAccountNumber())
                .message("Transfer found")
                .build();
    }

    public List<TransferResponse> getTransfers(){
        return transferRepository.findAll().stream()
                .map(transfer -> TransferResponse.builder()
                        .id(transfer.getId())
                        .status(transfer.getStatus())
                        .amount(transfer.getAmount())
                        .fromAccountNumber(transfer.getFromAccount().getAccountNumber())
                        .toAccountNumber(transfer.getToAccount().getAccountNumber())
                        .message("Transfer found")
                        .build())
                .collect(Collectors.toList());
    }

    public List<TransferResponse> getTransfersByAccountNumber(String accountNumber){
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(() -> new AccountNotFoundException("Account not found!"));
        List<Transfer> from = transferRepository.findByFromAccountNumber(accountNumber);
        List<Transfer> to = transferRepository.findByToAccountAccountNumber(accountNumber);
        List<Transfer> allTransfers = new ArrayList<>();
        allTransfers.addAll(from);
        allTransfers.addAll(to);
         return allTransfers.stream()
                 .map(transfer -> TransferResponse.builder()
                         .id(transfer.getId())
                         .fromAccountNumber(transfer.getFromAccount().getAccountNumber())
                         .toAccountNumber(transfer.getToAccount().getAccountNumber())
                         .status(transfer.getStatus())
                         .amount(transfer.getAmount())
                         .message("Transfer found")
                         .build()
                 ).toList();
    }

}
