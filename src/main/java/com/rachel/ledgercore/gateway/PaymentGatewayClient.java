package com.rachel.ledgercore.gateway;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentGatewayClient {
    PaymentGatewayResponse processPayment(UUID transferId, BigDecimal amount);
}
