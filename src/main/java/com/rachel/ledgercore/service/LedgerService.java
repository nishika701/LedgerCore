package com.rachel.ledgercore.service;

import com.rachel.ledgercore.exception.AccountNotFoundException;
import com.rachel.ledgercore.exception.TransferNotFoundException;
import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.model.LedgerEntry;
import com.rachel.ledgercore.model.Transfer;
import com.rachel.ledgercore.repository.AccountRepository;
import com.rachel.ledgercore.repository.LedgerEntryRepository;
import com.rachel.ledgercore.repository.TransferRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class LedgerService {

    private LedgerEntryRepository ledgerEntryRepository;
    private AccountRepository accountRepository;
    private TransferRepository transferRepository;

    LedgerService(LedgerEntryRepository ledgerEntryRepository,AccountRepository accountRepository,TransferRepository transferRepository){
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.accountRepository = accountRepository;
        this.transferRepository = transferRepository;
    }

    public List<LedgerEntry> getLedgerEntryByAccountNumber(String accountNumber){
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(()-> new AccountNotFoundException("Account not existing"));
        List<LedgerEntry> list = ledgerEntryRepository.findByAccount(account);
        return list;
    }

    public List<LedgerEntry> getLedgerEntryByTransferId(UUID id){
        Transfer transfer = transferRepository.findById(id).orElseThrow(()-> new TransferNotFoundException("Transfer not found"));
        List<LedgerEntry> list = ledgerEntryRepository.findByTransfer(transfer);
        return list;
    }

}
