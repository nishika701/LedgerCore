package com.rachel.ledgercore.model;

import com.rachel.ledgercore.enums.Status;
import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Transfer {

    @Id
    private UUID id;

    @PrePersist
    public void prePersist(){
        if(id == null){
            id = java.util.UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
    }

    @ManyToOne
    @JoinColumn(name="from_account_id",nullable = false)
    private Account fromAccount;

    @ManyToOne
    @JoinColumn(name="to_account_id",nullable = false)
    private Account toAccount;

    @Column(nullable = false)
    @Positive
    private BigDecimal amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false, unique = true)
    private String idempotencyKey;

    @Version
    private Long version;
}
