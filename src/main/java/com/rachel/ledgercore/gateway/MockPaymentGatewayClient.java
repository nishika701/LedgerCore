package com.rachel.ledgercore.gateway;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Component
public class MockPaymentGatewayClient implements PaymentGatewayClient {

    private boolean simulateFailure = false;
    private boolean simulateTimeout = false;

    public void setSimulateFailure(boolean simulateFailure){
        this.simulateFailure = simulateFailure;
    }

    public void setSimulateTimeout(boolean simulateTimeout){
        this.simulateTimeout = simulateTimeout;
    }

    @Override
    @CircuitBreaker(name = "paymentGateway", fallbackMethod = "processPaymentFallback")
    public PaymentGatewayResponse processPayment(UUID transferId, BigDecimal amount){
        log.info("Contacting external payment gateway for transferId: {}", transferId);

        if(simulateTimeout){
            try{
                Thread.sleep(3000);
            } catch (InterruptedException e){
                Thread.currentThread().interrupt();
            }
        }

        if(simulateFailure){
            log.warn("External payment gateway 500 INTERNAL SERVER ERROR!");
            throw new RuntimeException("External payment network unavailable (503 Service Unavailable)");
        }

        return PaymentGatewayResponse.builder()
                .successful(true)
                .transactionReference("EXT-TXN-" + UUID.randomUUID())
                .message("Authorized by external network")
                .build();
    }

    public PaymentGatewayResponse processPaymentFallback(UUID transferId, BigDecimal amount, Throwable t){
        log.error("Circuit Breaker Fallback triggered for transferId {}. Reason: {}", transferId, t.getMessage());
        return PaymentGatewayResponse.builder()
                .successful(false)
                .transactionReference(null)
                .message("Payment gateway unavailable - circuit breaker tripped: " + t.getMessage())
                .build();

    }
}
