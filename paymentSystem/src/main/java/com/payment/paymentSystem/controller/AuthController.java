package com.payment.paymentSystem.controller;

import com.payment.paymentSystem.entity.AuthRequest;
import com.payment.paymentSystem.entity.AuthResponse;
import com.payment.paymentSystem.security.JwtUtil;
import com.payment.paymentSystem.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/signup")
    public String signup(@RequestBody AuthRequest request) {
        authService.signup(request);
        return "User created";
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        AuthResponse token = authService.login(request);
        return token;
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody String refreshToken) {

        if (!jwtUtil.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        Long userId = jwtUtil.extractUserId(refreshToken);

        String newAccessToken = jwtUtil.generateAccessToken(userId);

        return new AuthResponse(newAccessToken, refreshToken);
    }
}
