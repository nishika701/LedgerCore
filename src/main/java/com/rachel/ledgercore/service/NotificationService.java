package com.rachel.ledgercore.service;

import com.rachel.ledgercore.exception.AccountNotFoundException;
import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.model.Notification;
import com.rachel.ledgercore.repository.AccountRepository;
import com.rachel.ledgercore.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AccountRepository accountRepository;

    public Notification getNotificationById(UUID notificationId){
        Notification notification = notificationRepository.findById(notificationId).orElseThrow(() -> new RuntimeException("Notification ID not found!"));
        return notification;
    }

    public List<Notification> getNotificationsByAccountNumber(String accountNumber){
        Account account = accountRepository.findByAccountNumber(accountNumber).orElseThrow(()-> new AccountNotFoundException("Account number not found"));
        List<Notification> list = notificationRepository.findByAccount(account);
        return list;
    }

}
