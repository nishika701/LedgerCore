package com.rachel.ledgercore.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteAccountResponse {

    @NotBlank
    String accountNumber;

    @NotBlank
    String message;

    @NotNull
    LocalDateTime deletedOn;
}
