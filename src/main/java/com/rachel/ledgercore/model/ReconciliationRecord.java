package com.rachel.ledgercore.model;

import com.rachel.ledgercore.enums.ReconciliationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reconciliation_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReconciliationRecord {

    @Id
    private UUID id;

    @PrePersist
    public void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (reconciledAt == null) {
            reconciledAt = LocalDateTime.now();
        }
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull
    private BigDecimal recordedBalance;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull
    private BigDecimal calculatedBalance;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull
    private BigDecimal discrepancyAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @NotNull
    private ReconciliationStatus status;

    @Column(nullable = false)
    private LocalDateTime reconciledAt;

    @Column(length = 500)
    private String details;
}
