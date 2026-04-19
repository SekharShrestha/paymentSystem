package com.payment.paymentSystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

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

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;
}
