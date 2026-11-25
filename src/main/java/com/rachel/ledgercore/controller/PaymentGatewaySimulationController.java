package com.rachel.ledgercore.controller;

import com.rachel.ledgercore.gateway.MockPaymentGatewayClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/gateway")
public class PaymentGatewaySimulationController {

    private final MockPaymentGatewayClient mockPaymentGatewayClient;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @GetMapping("/status")
    public ResponseEntity<Map<String,Object>> getStatus(){
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("paymentGateway");
        return ResponseEntity.ok(Map.of(
                "simulateFailure", mockPaymentGatewayClient.isSimulateFailure(),
                "simulateTimeout", mockPaymentGatewayClient.isSimulateTimeout(),
                "circuitBreakerState",cb.getState().name(),
                "failureRate", cb.getMetrics().getFailureRate() + "%"
        ));
    }

   @PostMapping("/simulate-outage")
   public ResponseEntity<Map<String, Object>> setOutage(@RequestParam boolean enabled){
        mockPaymentGatewayClient.setSimulateFailure(enabled);
        return ResponseEntity.ok(Map.of(
                "message", enabled ? "External gateway outage SIMULATED (503 errors enabled)" : "External gateway RESTORED to healthy" , "simulateFailure" , enabled
        ));
   }

   @PostMapping("/simulate-timeout")
    public ResponseEntity<Map<String,Object>> setTimeout(@RequestParam boolean enabled){
        mockPaymentGatewayClient.setSimulateTimeout(enabled);
        return ResponseEntity.ok(Map.of(
                "message", enabled ? "Network timeout SIMULATED (3s latency enabled)" : "Network latency RESTORED to normal",
                "simulateTimeout", enabled
        ));
   }

    @PostMapping("/reset-circuit-breaker")
    public ResponseEntity<Map<String, String>> resetCircuitBreaker() {
        circuitBreakerRegistry.circuitBreaker("paymentGateway").reset();
        return ResponseEntity.ok(Map.of("message", "Circuit breaker reset to CLOSED state"));
    }
}
