package com.payment.paymentSystem.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    private String idempotencyKey;

    private Long fromUserId;
    private Long toUserId;
    private BigDecimal amount;
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
}
