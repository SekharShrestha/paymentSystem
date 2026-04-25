package com.payment.paymentSystem.entity;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {

    private String transactionId;
    private Long fromUserId;
    private Long toUserId;
    private BigDecimal amount;
    private String type; // DEBIT / CREDIT
}
