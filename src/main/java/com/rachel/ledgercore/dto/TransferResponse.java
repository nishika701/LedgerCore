package com.rachel.ledgercore.dto;

import com.rachel.ledgercore.enums.Status;
import jakarta.persistence.Entity;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

import java.math.BigDecimal;

@Builder
@Data
public class TransferResponse {

    private UUID id;
    private Status status;
    private BigDecimal amount;
    private String message;
}
