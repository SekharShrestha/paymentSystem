package com.payment.paymentSystem.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {

    private Long fromUserId;
    private Long toUserId;
    private BigDecimal amount;
}
