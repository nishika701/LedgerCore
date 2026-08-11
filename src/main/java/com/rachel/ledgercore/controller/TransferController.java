package com.rachel.ledgercore.controller;

import com.rachel.ledgercore.dto.TransferRequest;
import com.rachel.ledgercore.dto.TransferResponse;
import com.rachel.ledgercore.model.Transfer;
import com.rachel.ledgercore.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class TransferController {

    private final TransferService transferService;

    @PostMapping("/transfers")
    public ResponseEntity<TransferResponse> transferMoney(@Valid @RequestBody TransferRequest transferRequest){
        TransferResponse transferResponse = transferService.transferMoney(transferRequest);
        return ResponseEntity.ok(transferResponse);
    }

    @GetMapping("/{transferId}")
    public ResponseEntity<TransferResponse> getTransferByTransferId(@PathVariable UUID transferId){
        TransferResponse transferResponse = transferService.getTransferByTransferId(transferId);
        return ResponseEntity.ok(transferResponse);
    }

    @GetMapping("/transfers")
    public ResponseEntity<List<TransferResponse>> getTransfers(){
        List<TransferResponse> transferResponses = transferService.getTransfers();
        return ResponseEntity.ok(transferResponses);
    }

    @GetMapping("/accounts/{accountNumber}/transfers")
    public ResponseEntity<List<TransferResponse>> getTransfersByAccountNumber(@PathVariable String accountNumber){
        List<TransferResponse> responses = transferService.getTransfersByAccountNumber(accountNumber);
        return ResponseEntity.ok(responses);
    }

}
/*
@Controller
@ResponseBody is same as @RestController
 */

/*
public TransferController(TransferService transferService){
        this.transferService = transferService;
    } not required if @RequiredArgsConstructor is used, it will generate constructor with final fields
 */