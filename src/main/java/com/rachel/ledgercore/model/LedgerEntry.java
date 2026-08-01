package com.rachel.ledgercore.model;

import com.rachel.ledgercore.enums.EntryType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LedgerEntry {

    @Id
    private UUID id;

    @PrePersist
    public void prePersist(){
        if(id == null){
            id = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
    }

    @Column(nullable = true)
    private String description;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "account_id",nullable = false)
    private Account account;

    @Column(nullable = false)
    @NotNull
    @Enumerated(EnumType.STRING)
    private EntryType entryType;

    @Column(nullable = false,precision = 19, scale = 2)
    @Positive
    @NotNull
    private BigDecimal amount;

    @ManyToOne
    @NotNull
    @JoinColumn(name = "transfer_id",nullable = false)
    private Transfer transfer;

    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;
}
