package com.rachel.ledgercore.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentGatewayResponse {
    private boolean successful;
    private String transactionReference;
    private String message;
}
