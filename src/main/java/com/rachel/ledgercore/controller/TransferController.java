package com.rachel.ledgercore.controller;

import com.rachel.ledgercore.dto.TransferRequest;
import com.rachel.ledgercore.dto.TransferResponse;
import com.rachel.ledgercore.service.TransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transfer")
public class TransferController {

    private final TransferService transferService;

    @PostMapping
    public ResponseEntity<TransferResponse> transferMoney(@Valid @RequestBody TransferRequest transferRequest){
        TransferResponse transferResponse = transferService.transferMoney(transferRequest);
        return ResponseEntity.ok(transferResponse);
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