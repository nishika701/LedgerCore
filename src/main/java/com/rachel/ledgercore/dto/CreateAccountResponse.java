package com.rachel.ledgercore.dto;

import com.rachel.ledgercore.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateAccountResponse {

    private UUID id;

    private Currency currency;

    private String accountNumber;

    private String ownerName;

    private BigDecimal balance;

    private String message;

    private LocalDateTime createdAt;
}
