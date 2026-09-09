package com.rachel.ledgercore.repository;

import com.rachel.ledgercore.model.Account;
import com.rachel.ledgercore.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByAccount(Account account);
}
