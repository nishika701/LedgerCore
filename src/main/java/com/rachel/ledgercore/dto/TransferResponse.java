package com.rachel.ledgercore.dto;

import com.rachel.ledgercore.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransferResponse {

    private UUID id;

    private String fromAccountNumber;

    private String toAccountNumber;

    private Status status;

    private BigDecimal amount;

    private String message;
}