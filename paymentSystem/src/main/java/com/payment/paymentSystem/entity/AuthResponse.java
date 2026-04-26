package com.payment.paymentSystem.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
}
