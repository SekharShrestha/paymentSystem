package com.payment.paymentSystem.controller;

import com.payment.paymentSystem.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/add")
    public String addMoney(@RequestParam Long userId,
                           @RequestParam BigDecimal amount) {

        walletService.addMoney(userId, amount);
        return "Money added successfully";
    }

    @GetMapping("/balance")
    public BigDecimal getBalance(@RequestParam Long userId) {
        return walletService.getBalance(userId);
    }
}
