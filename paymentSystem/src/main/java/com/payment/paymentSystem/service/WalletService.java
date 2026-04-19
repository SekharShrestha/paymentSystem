package com.payment.paymentSystem.service;

import com.payment.paymentSystem.entity.*;
import com.payment.paymentSystem.repository.LedgerRepository;
import com.payment.paymentSystem.repository.TransactionRepository;
import com.payment.paymentSystem.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public void addMoney(Long userId, BigDecimal amount) {

        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));

        // Update balance
        wallet.setBalance(wallet.getBalance().add(amount));
        walletRepository.save(wallet);

        // Ledger entry (source of truth)
        Ledger ledger = Ledger.builder()
                .walletId(wallet.getId())
                .amount(amount)
                .type(TransactionType.CREDIT)
                .createdAt(LocalDateTime.now())
                .build();

        ledgerRepository.save(ledger);
    }

    public BigDecimal getBalance(Long userId) {
        return walletRepository.findByUserId(userId)
                .map(Wallet::getBalance)
                .orElseThrow(() -> new RuntimeException("Wallet not found"));
    }

    @Transactional
    public void transferMoney(String idempotencyKey,
                              Long fromUserId,
                              Long toUserId,
                              BigDecimal amount) {

        // 1️⃣ Check if already processed
        Transaction existing = transactionRepository.findById(idempotencyKey).orElse(null);

        if (existing != null) {
            if (existing.getStatus() == TransactionStatus.SUCCESS) {
                return; // already done
            } else {
                throw new RuntimeException("Transaction already in progress or failed");
            }
        }

        // 2️⃣ Create transaction record
        Transaction txn = Transaction.builder()
                .idempotencyKey(idempotencyKey)
                .fromUserId(fromUserId)
                .toUserId(toUserId)
                .amount(amount)
                .status(TransactionStatus.INITIATED)
                .build();

        transactionRepository.save(txn);

        try {
            // 3️⃣ Perform transfer (same logic as before)
            Wallet sender = walletRepository.findByUserIdForUpdate(fromUserId)
                    .orElseThrow(() -> new RuntimeException("Sender not found"));

            Wallet receiver = walletRepository.findByUserIdForUpdate(toUserId)
                    .orElseThrow(() -> new RuntimeException("Receiver not found"));

            if (sender.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient balance");
            }

            sender.setBalance(sender.getBalance().subtract(amount));
            receiver.setBalance(receiver.getBalance().add(amount));

            walletRepository.save(sender);
            walletRepository.save(receiver);

            // ledger entries...

            txn.setStatus(TransactionStatus.SUCCESS);

        } catch (Exception e) {
            txn.setStatus(TransactionStatus.FAILED);
            throw e;
        }
    }
}
