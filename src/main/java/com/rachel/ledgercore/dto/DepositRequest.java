package com.rachel.ledgercore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Builder
@Data
public class DepositRequest {

    @NotBlank
    private String accountNumber;

    @NotNull
    @Positive
    private BigDecimal amount;

}
