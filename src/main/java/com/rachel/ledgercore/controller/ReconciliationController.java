package com.rachel.ledgercore.controller;

import com.rachel.ledgercore.model.ReconciliationRecord;
import com.rachel.ledgercore.service.ReconciliationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/reconciliation")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    @PostMapping("/run")
    public ResponseEntity<List<ReconciliationRecord>> runFullAudit() {
        List<ReconciliationRecord> records = reconciliationService.reconcileAllAccounts();
        return ResponseEntity.ok(records);
    }

    @PostMapping("/account/{accountId}")
    public ResponseEntity<ReconciliationRecord> auditAccount(@PathVariable UUID accountId){
        ReconciliationRecord record = reconciliationService.reconcileAccount(accountId);
        return ResponseEntity.ok(record);
    }
}
