package com.rachel.ledgercore.dto;


import com.rachel.ledgercore.enums.Currency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateAccountRequest {
    @NotBlank
    private String ownerName;

    @NotBlank
    private String accountNumber;

    @NotNull
    private Currency currency;
}
