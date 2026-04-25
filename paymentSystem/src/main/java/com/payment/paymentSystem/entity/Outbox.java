package com.payment.paymentSystem.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Outbox {

    @Id
    @GeneratedValue
    private Long id;

    private String aggregateId; // transactionId

    private String type; // DEBIT / CREDIT

    @Column(columnDefinition = "TEXT")
    private String payload;

    private boolean published;

    private LocalDateTime createdAt;
}
