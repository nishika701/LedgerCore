package com.rachel.ledgercore.controller;

import com.rachel.ledgercore.model.LedgerEntry;
import com.rachel.ledgercore.service.LedgerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1")
public class LedgerController {

    private final LedgerService ledgerService;

    LedgerController(LedgerService ledgerService){
        this.ledgerService = ledgerService;
    }

    @GetMapping("/accounts/{accountNumber}/ledger")
    public ResponseEntity<List<LedgerEntry>> getLedgerEntryByAccountNumber(@PathVariable String accountNumber){
        List<LedgerEntry> entry = ledgerService.getLedgerEntryByAccountNumber(accountNumber);
        return ResponseEntity.ok(entry);
    }


    @GetMapping("/transfers/{transferId}/ledger")
    public ResponseEntity<List<LedgerEntry>> getLedgerEntryByTransferId(@PathVariable UUID transferId){
        List<LedgerEntry> entry = ledgerService.getLedgerEntryByTransferId(transferId);
        return ResponseEntity.ok(entry);
    }

}
