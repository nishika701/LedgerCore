package com.rachel.ledgercore.dto;


import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class TransferRequest {

    private String fromAccountNumber;

    private String toAccountNumber;

    private BigDecimal amount;

    private String idempotencyKey;
}
