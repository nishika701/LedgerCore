package com.rachel.ledgercore.controller;

import com.rachel.ledgercore.model.Notification;
import com.rachel.ledgercore.service.NotificationService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/notifications/{notificationId}")
    public ResponseEntity<Notification> getNotificationById(@PathVariable UUID notificationId){
        Notification notification = notificationService.getNotificationById(notificationId);
        return ResponseEntity.ok(notification);
    }

    @GetMapping("/accounts/{accountNumber}/notifications")
    public ResponseEntity<List<Notification>> getNotificationByAccountNumber(@PathVariable String accountNumber){
        List<Notification> notification = notificationService.getNotificationsByAccountNumber(accountNumber);
        return ResponseEntity.ok(notification);
    }

}
