package com.rachel.ledgercore.model;

import com.rachel.ledgercore.enums.Currency;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.*;
import java.util.UUID;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "accounts")
public class Account {

    @Id
    private UUID id;

    @PrePersist
    public void prePersist(){
        if(id == null){
            id = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Currency currency;

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    @NotBlank
    private String ownerName;

    @Column(nullable = false, precision = 19, scale = 2)
    @NotNull
    @PositiveOrZero
    private BigDecimal balance;

    @Version
    private Long version;

}
