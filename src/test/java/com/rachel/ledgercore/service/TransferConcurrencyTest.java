package com.rachel.ledgercore.service;

import com.rachel.ledgercore.dto.TransferRequest;
import com.rachel.ledgercore.enums.Currency;
import com.rachel.ledgercore.enums.Role;
import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.model.User;
import com.rachel.ledgercore.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
public class TransferConcurrencyTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @AfterEach
    void tearDown(){
        SecurityContextHolder.clearContext();
        ledgerEntryRepository.deleteAll();
        notificationRepository.deleteAll();
        transferRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldMaintainConservationOfMoneyUnderConcurrentTransfers() throws InterruptedException{

        User user = userRepository.save(User.builder()
                .username("concurrent-user-" + UUID.randomUUID())
                .password("password")
                .role(Role.USER)
                .build());

        Account sender = accountRepository.save(Account.builder()
                .currency(Currency.INR)
                .accountNumber("SENDER-" + UUID.randomUUID())
                .ownerName("Sender")
                .user(user)
                .balance(new BigDecimal("1000.0"))
                .build());

        Account receiver = accountRepository.save(Account.builder()
                .currency(Currency.INR)
                .accountNumber("RECEIVER-" + UUID.randomUUID())
                .ownerName("Receiver")
                .user(user)
                .balance(BigDecimal.ZERO)
                .build());

        int threadCount = 10;
        BigDecimal transferAmount = new BigDecimal("50.00");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for(int i = 0; i < threadCount; i++){
            executor.submit(()-> {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(user.getUsername(),null)
                );

                readyLatch.countDown();

                try{
                    startLatch.await();

                    TransferRequest request = TransferRequest.builder()
                            .fromAccountNumber(sender.getAccountNumber())
                            .toAccountNumber(receiver.getAccountNumber())
                            .amount(transferAmount)
                            .idempotencyKey(UUID.randomUUID().toString())
                            .build();

                    transferService.transferMoney(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        finishLatch.await();
        executor.shutdown();

        Account updatedSender =  accountRepository.findByAccountNumber(sender.getAccountNumber()).orElseThrow();
        Account updatedReceiver = accountRepository.findByAccountNumber(receiver.getAccountNumber()).orElseThrow();

        BigDecimal totalAfterTransfers = updatedSender.getBalance().add(updatedReceiver.getBalance());

        System.out.println("====== CONCURRENCY TEST RESULTS ======");
        System.out.println("Successful transfers: " + successCount.get());
        System.out.println("Failed transfers (Version Collisions): " + failureCount.get());
        System.out.println("Sender balance: " + updatedSender.getBalance());
        System.out.println("Receiver balance: " + updatedReceiver.getBalance());
        System.out.println("Total money in system: " + totalAfterTransfers);
        System.out.println("======================================");
        assertEquals(0, new BigDecimal("1000.00").compareTo(totalAfterTransfers),
                "Conservation of money violated! Total balances changed.");
        BigDecimal expectedReceiverBalance = transferAmount.multiply(new BigDecimal(successCount.get()));
        assertEquals(0, expectedReceiverBalance.compareTo(updatedReceiver.getBalance()),
                "Receiver balance does not match total successful transfer amounts.");
    }

    @Test
    void shouldExecuteOnlyOnceWhenIdenticalIdempotencyKeyIsSentConcurrently() throws InterruptedException {

        User user = userRepository.save(User.builder()
                .username("idemp-user-" + UUID.randomUUID())
                .password("password")
                .role(Role.USER)
                .build());

        Account sender = accountRepository.save(Account.builder()
                .currency(Currency.INR)
                .accountNumber("IDEMP-SENDER-" + UUID.randomUUID())
                .ownerName("Sender")
                .user(user)
                .balance(new BigDecimal("1000.00"))
                .build());

        Account receiver = accountRepository.save(Account.builder()
                .currency(Currency.INR)
                .accountNumber("IDEMP-RECEIVER-" + UUID.randomUUID())
                .ownerName("Receiver")
                .user(user)
                .balance(new BigDecimal("0"))
                .build());

        String sharedIdempotencyKey = "DUPLICATE-KEY-" + UUID.randomUUID();
        BigDecimal transferAmount = new BigDecimal("100.0");

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger executedCount = new AtomicInteger(0);

        for(int i = 0;i < threadCount;i++){
            executor.submit(() -> {
               SecurityContextHolder.getContext().setAuthentication(
                       new UsernamePasswordAuthenticationToken(user.getUsername(),null)
               );

               readyLatch.countDown();
               try{
                   startLatch.await();

                   TransferRequest request = TransferRequest.builder()
                           .fromAccountNumber(sender.getAccountNumber())
                           .toAccountNumber(receiver.getAccountNumber())
                           .amount(transferAmount)
                           .idempotencyKey(sharedIdempotencyKey)
                           .build();

                   transferService.transferMoney(request);
                   executedCount.incrementAndGet();
               } catch (Exception e) {
                   //throw new RuntimeException(e);
               } finally {
                   SecurityContextHolder.clearContext();
                   finishLatch.countDown();
               }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        finishLatch.await();
        executor.shutdown();

        Account updatedSender = accountRepository.findByAccountNumber(sender.getAccountNumber()).orElseThrow();
        Account updatedReceiver = accountRepository.findByAccountNumber(receiver.getAccountNumber()).orElseThrow();
        System.out.println("====== IDEMPOTENCY STORM RESULTS ======");
        System.out.println("Sender balance (must be 900.00): " + updatedSender.getBalance());
        System.out.println("Receiver balance (must be 100.00): " + updatedReceiver.getBalance());
        System.out.println("=======================================");

        assertEquals(0, new BigDecimal("900.00").compareTo(updatedSender.getBalance()),
                "Sender was charged more than once!");

        assertEquals(0, new BigDecimal("100.00").compareTo(updatedReceiver.getBalance()),
                "Receiver was credited more than once!");

        var transfers = transferRepository.findByIdempotencyKey(sharedIdempotencyKey);
        assertEquals(true, transfers.isPresent(), "Transfer was not saved!");

        var entries = ledgerEntryRepository.findByTransfer(transfers.get());
        assertEquals(2, entries.size(), "Duplicate ledger entries were created!");
    }

    @Test
    void shouldPreventOverdraftUnderConcurrentTransfers() throws InterruptedException {

        User user = userRepository.save(User.builder()
                .username("overdraft-user-" + UUID.randomUUID())
                .password("password")
                .role(Role.USER)
                .build());

        Account sender = accountRepository.save(Account.builder()
                .accountNumber("OVERDRAFT-SENDER-" + UUID.randomUUID())
                .ownerName("Alice")
                .currency(Currency.INR)
                .balance(new BigDecimal("100.00")) // Only 100.00!
                .user(user)
                .build());

        Account receiver = accountRepository.save(Account.builder()
                .accountNumber("OVERDRAFT-RECEIVER-" + UUID.randomUUID())
                .ownerName("Bob")
                .currency(Currency.INR)
                .balance(BigDecimal.ZERO)
                .user(user)
                .build());

        int threadCount = 5;
        BigDecimal transferAmount = new BigDecimal("80.00");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(user.getUsername(), null)
                );
                readyLatch.countDown();
                try {
                    startLatch.await(); // Simultaneous attack
                    TransferRequest request = TransferRequest.builder()
                            .fromAccountNumber(sender.getAccountNumber())
                            .toAccountNumber(receiver.getAccountNumber())
                            .amount(transferAmount)
                            .idempotencyKey(UUID.randomUUID().toString())
                            .build();
                    transferService.transferMoney(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    rejectedCount.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                    finishLatch.countDown();
                }
            });
        }
        readyLatch.await();
        startLatch.countDown();
        finishLatch.await();
        executor.shutdown();

        Account updatedSender = accountRepository.findByAccountNumber(sender.getAccountNumber()).orElseThrow();
        Account updatedReceiver = accountRepository.findByAccountNumber(receiver.getAccountNumber()).orElseThrow();

        System.out.println("====== OVERDRAFT TEST RESULTS ======");
        System.out.println("Successful transfers (must be 1): " + successCount.get());
        System.out.println("Rejected transfers (must be 4): " + rejectedCount.get());
        System.out.println("Alice's final balance (must be 20.00): " + updatedSender.getBalance());
        System.out.println("Bob's final balance (must be 80.00): " + updatedReceiver.getBalance());
        System.out.println("====================================");


        assertEquals(1, successCount.get(), "More than one transfer succeeded, causing overdraft!");

        assertEquals(0, new BigDecimal("20.00").compareTo(updatedSender.getBalance()),
                "Alice's balance was incorrectly calculated!");

        assertEquals(true, updatedSender.getBalance().compareTo(BigDecimal.ZERO) >= 0,
                "CRITICAL BUG: Account balance dropped below zero!");
    }

    @Test
    void shouldPreventDeadlocksWhenTwoAccountsTransferToEachOtherConcurrently() throws InterruptedException {

        User user1 = userRepository.save(User.builder().username("user1-" + UUID.randomUUID())
                .password("password")
                .role(Role.USER)
                .build());

        User user2 = userRepository.save(User.builder().username("user2-" + UUID.randomUUID())
                .password("password")
                .role(Role.USER)
                .build());

        Account user1_account = accountRepository.save(Account.builder()
                .currency(Currency.INR)
                .accountNumber("user1" + UUID.randomUUID())
                .ownerName("user1")
                .user(user1)
                .balance(new BigDecimal("1000.00"))
                .build());

        Account user2_account = accountRepository.save(Account.builder()
                .currency(Currency.INR)
                .accountNumber("user2" + UUID.randomUUID())
                .ownerName("user2")
                .user(user2)
                .balance(new BigDecimal("1000.00"))
                .build());

        int threadCount = 10;
        BigDecimal transferAmount = new BigDecimal("50.00");

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for(int i = 0;i < threadCount;i++){
            final boolean isUser1ToUser2 = (i % 2 == 0);

            executor.submit(() -> {
                User senderUser = isUser1ToUser2 ? user1 : user2;
                Account fromAcc = isUser1ToUser2 ? user1_account : user2_account;
                Account toAcc = isUser1ToUser2 ? user2_account : user1_account;

                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(senderUser.getUsername(), null)
                );

                readyLatch.countDown();
                try{
                    startLatch.await();

                    TransferRequest request = TransferRequest.builder()
                            .fromAccountNumber(fromAcc.getAccountNumber())
                            .toAccountNumber(toAcc.getAccountNumber())
                            .amount(transferAmount)
                            .idempotencyKey(UUID.randomUUID().toString())
                            .build();

                    transferService.transferMoney(request);
                    successCount.incrementAndGet();
                } catch (Exception e){
                    failureCount.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                    finishLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        finishLatch.await();
        executor.shutdown();

        Account updatedUser1 = accountRepository.findByAccountNumber(user1_account.getAccountNumber()).orElseThrow();
        Account updatedUser2 = accountRepository.findByAccountNumber(user2_account.getAccountNumber()).orElseThrow();

        BigDecimal totalAfterTransfers = updatedUser1.getBalance().add(updatedUser2.getBalance());

        System.out.println("====== BIDIRECTIONAL TEST RESULTS ======");
        System.out.println("Successful transfers: " + successCount.get());
        System.out.println("Failed/Collided transfers: " + failureCount.get());
        System.out.println("Alice's balance: " + updatedUser1.getBalance());
        System.out.println("Bob's balance: " + updatedUser2.getBalance());
        System.out.println("Total money in system: " + totalAfterTransfers);
        System.out.println("========================================");

        assertEquals(0, new BigDecimal("2000.00").compareTo(totalAfterTransfers),
                "Conservation of money violated in bidirectional transfers!");
    }
}
