# ADR 001: Concurrency Control and Locking Strategy for Account Balances

## Status
**Accepted** (2026-11-10)

## Context & Problem Statement
LedgerCore processes financial transfers between accounts. When multiple transactions hit the same account simultaneously (e.g., simultaneous payments or rapid withdrawals), the system must guarantee:
1. **Conservation of Money**: $D - C = 0$ across all transfers. Money must never be created or destroyed.
2. **No Lost Updates**: Concurrent balance deductions must not overwrite each other.
3. **Overdraft Prevention**: An account balance must never drop below zero under concurrent race conditions.
4. **Deadlock Freedom**: Bidirectional transfers (Alice $\to$ Bob and Bob $\to$ Alice occurring at the exact same millisecond) must not trigger database deadlocks.

## Decision Drivers
* Financial data integrity is a non-negotiable hard invariant.
* High read throughput on non-contentious accounts without locking overhead.
* Resiliency under collision: transient version conflicts should self-heal without failing the user experience.
* Clear migration path towards pessimistic locking (`SELECT ... FOR UPDATE` ordered by ID) if contention on hot accounts exceeds thresholds.

## Considered Options
1. **No Locking (Naive Update)**: Calculate `balance = balance - amount` in application memory and save.
2. **Pessimistic Locking (`SELECT ... FOR UPDATE`)**: Lock account rows in the database for the duration of the transaction.
3. **Optimistic Locking with Bounded Exponential Retry (Chosen)**: Use Hibernate `@Version` on `Account` with `spring-retry` exponential backoff.

## Decision Outcome
We chose **Option 3: Optimistic Locking with Bounded Exponential Retry**.

### Implementation Details:
1. **Version Tracking**: Added `@Version private Long version;` to `Account` and `Transfer` entities.
2. **Self-Healing Retries**: Configured `@Retryable` on `TransferService.transferMoney()`:
   * Catches `ObjectOptimisticLockingFailureException`.
   * Retries up to 5 times.
   * Exponential backoff starting at 50ms with a $2\times$ multiplier (`@Backoff(delay = 50, multiplier = 2)`).
3. **Database Guardrails**: Added schema-level check constraint `CHECK (balance >= 0)` via Flyway migration `V1__init_schema.sql` to guarantee non-negative balances at the engine level.
4. **Idempotency Guard**: UNIQUE database constraint on `transfer.idempotency_key` ensures concurrent duplicate requests execute exactly once.

## Evaluation & Test Evidence
We proved this strategy by implementing four multi-threaded stress tests using `CountDownLatch` and `ExecutorService` (10 concurrent threads):
* `shouldMaintainConservationOfMoneyUnderConcurrentTransfers`: Proved total money is conserved under 10 parallel transfers against a single account.
* `shouldExecuteOnlyOnceWhenIdenticalIdempotencyKeyIsSentConcurrently`: Proved that 10 simultaneous identical idempotency keys execute exactly once and safely reject duplicates.
* `shouldPreventOverdraftUnderConcurrentTransfers`: Proved that 5 simultaneous withdrawals exceeding the account balance result in strictly non-negative balances with exact remaining balance.
* `shouldPreventDeadlocksWhenTwoAccountsTransferToEachOtherConcurrently`: Proved that bidirectional concurrent transfers resolve cleanly without PostgreSQL deadlocks.

## Future Evolution (Stage 2 Trigger)
If metrics in Stage 2 (Prometheus / Micrometer) show that optimistic retry rates on specific "hot" accounts exceed 15%, we will introduce pessimistic locking ordered by Account ID for those high-contention accounts as benchmarked in ADR 002.
