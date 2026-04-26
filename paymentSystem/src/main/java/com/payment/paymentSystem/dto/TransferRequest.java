package com.payment.paymentSystem.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferRequest {
    private Long toUserId;
    @NotNull
    @Positive
    private BigDecimal amount;
}
