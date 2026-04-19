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

        // 1️⃣ Check existing transaction
        Transaction txn = transactionRepository.findById(idempotencyKey).orElse(null);

        if (txn != null) {

            if (txn.getStatus() == TransactionStatus.SUCCESS) {
                return; // already processed
            }

            if (txn.getStatus() == TransactionStatus.PROCESSING) {
                throw new RuntimeException("Transaction already in progress");
            }

            if (txn.getStatus() == TransactionStatus.FAILED) {
                // retry allowed → continue
            }

        } else {
            // create new transaction
            txn = Transaction.builder()
                    .idempotencyKey(idempotencyKey)
                    .fromUserId(fromUserId)
                    .toUserId(toUserId)
                    .amount(amount)
                    .status(TransactionStatus.INITIATED)
                    .build();

            transactionRepository.save(txn);
        }

        try {
            // 2️⃣ Mark as PROCESSING
            txn.setStatus(TransactionStatus.PROCESSING);
            transactionRepository.save(txn);

            // 🔒 3️⃣ Ordered locking (deadlock prevention)
            Long first = Math.min(fromUserId, toUserId);
            Long second = Math.max(fromUserId, toUserId);

            Wallet firstWallet = walletRepository.findByUserIdForUpdate(first)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));

            Wallet secondWallet = walletRepository.findByUserIdForUpdate(second)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));

            Wallet sender = fromUserId.equals(first) ? firstWallet : secondWallet;
            Wallet receiver = toUserId.equals(first) ? firstWallet : secondWallet;

            // 💸 4️⃣ Validate
            if (sender.getBalance().compareTo(amount) < 0) {
                throw new RuntimeException("Insufficient balance");
            }

            // ➖ ➕ 5️⃣ Update balances
            sender.setBalance(sender.getBalance().subtract(amount));
            receiver.setBalance(receiver.getBalance().add(amount));

            walletRepository.save(sender);
            walletRepository.save(receiver);

            // 📒 6️⃣ Ledger entries
            Ledger debit = Ledger.builder()
                    .walletId(sender.getId())
                    .amount(amount)
                    .type(TransactionType.DEBIT)
                    .createdAt(LocalDateTime.now())
                    .build();

            Ledger credit = Ledger.builder()
                    .walletId(receiver.getId())
                    .amount(amount)
                    .type(TransactionType.CREDIT)
                    .createdAt(LocalDateTime.now())
                    .build();

            ledgerRepository.save(debit);
            ledgerRepository.save(credit);

            // ✅ 7️⃣ Mark success
            txn.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(txn);

        } catch (Exception e) {

            // ❌ 8️⃣ Mark failure
            txn.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(txn);

            throw e;
        }
    }
}
