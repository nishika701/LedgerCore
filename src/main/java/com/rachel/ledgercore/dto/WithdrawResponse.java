package com.rachel.ledgercore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class WithdrawResponse {

    @NotBlank
    String accountNumber;

    @Positive
    @NotNull
    BigDecimal balance;

    @NotBlank
    String message;
}