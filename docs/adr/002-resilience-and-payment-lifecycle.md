# ADR 002: Three-State Payment Lifecycle and Circuit Breaker Integration

## Status
**Accepted** (2026-11-14)

## Context & Problem Statement
In the initial iteration of LedgerCore, transfers occurred instantaneously within a single database transaction (`subtract from Alice` $\to$ `add to Bob` $\to$ `status = COMPLETED`). 

However, real-world payment networks (Visa, ACH, UPI, anti-fraud services, or third-party gateways) are external distributed systems. They can suffer from:
1. **Network Latency & Timeouts**: External calls taking several seconds can exhaust application server threads (thread pool starvation), leading to cascading system-wide outages.
2. **Third-Party 500 Outages**: If an external gateway fails mid-transaction, naive implementations risk deducting money without settlement, violating financial integrity.
3. **Thundering Herd**: Repeatedly hammering a failing downstream gateway prevents it from recovering.

## Decision Drivers
* **Financial Safety**: Money must NEVER be deducted from a sender if external authorization fails.
* **Fail-Fast Resiliency**: When a downstream partner is down, our system must reject calls in $< 1\text{ms}$ rather than blocking worker threads for 10–30 seconds.
* **Audit Trail Integrity**: `LedgerEntry` double-entry records must only be written for verified, settled transactions.

## Considered Options
1. **Instantaneous Single-Phase Transfer (Status Quo)**: Move funds directly without modeling external network states.
   * *Problem*: Unrealistic in production fintech; vulnerable to external failures.
2. **Distributed 2-Phase Commit (2PC)**: Coordinate distributed transactions across services.
   * *Problem*: Excessive latency, complex coordinator failure modes, and blocked database rows.
3. **Three-State Payment Lifecycle with Resilience4j Circuit Breaker (Chosen)**:
   * State 1: `PENDING` (Record intent in DB before external call).
   * External Call: Protected by Resilience4j `@CircuitBreaker` and fast fallback.
   * State 2 (Success): Transition to `COMPLETED`, deduct/credit balances, persist balanced `DEBIT`/`CREDIT` ledger entries.
   * State 3 (Failure): Transition to `FAILED`, leave balances 100% untouched, persist **zero** ledger entries.

## Decision Outcome
We chose **Option 3**.

### Implementation Details:
1. **State Machine**:
   - `Transfer.status` transitions from `PENDING` $\to$ `COMPLETED` or `FAILED`.
2. **Resilience4j Circuit Breaker (`paymentGateway`)**:
   - `sliding-window-size = 10`: Evaluates error rate across the last 10 requests.
   - `failure-rate-threshold = 50%`: If 5 out of 10 calls fail, circuit breaker trips to `OPEN`.
   - `wait-duration-in-open-state = 5s`: Fails fast for 5 seconds without touching the network.
   - `permitted-number-of-calls-in-half-open-state = 3`: Probes downstream recovery with 3 test requests before closing.
3. **Fast Fallback**:
   - `MockPaymentGatewayClient.processPaymentFallback()` intercepts calls when the circuit is `OPEN`, returning an immediate failure response without thread blocking.

## Evaluation & Test Evidence
Implemented integration tests in `PaymentResilienceTest.java`:
* `shouldFailGracefullyAndPreserveBalancesWhenGatewayFails`: Proved that when the gateway fails, the transfer is marked `FAILED`, the sender's balance is untouched (₹1000.00), receiver is untouched (₹0.00), and zero ledger entries are created.
* `shouldTripCircuitBreakerToOpenWhenFailureRateExceedsThreshold`: Proved that after 10 consecutive failures, the circuit breaker automatically transitioned from `CLOSED` to `OPEN` state.
