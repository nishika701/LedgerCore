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

}
