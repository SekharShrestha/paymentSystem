package com.payment.paymentSystem.service;

import com.payment.paymentSystem.entity.Ledger;
import com.payment.paymentSystem.entity.TransactionType;
import com.payment.paymentSystem.entity.Wallet;
import com.payment.paymentSystem.repository.LedgerRepository;
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
    public void transferMoney(Long fromUserId, Long toUserId, BigDecimal amount) {

        if (fromUserId.equals(toUserId)) {
            throw new RuntimeException("Cannot transfer to same user");
        }

        // 🔒 Lock both wallets
        Wallet sender = walletRepository.findByUserIdForUpdate(fromUserId)
                .orElseThrow(() -> new RuntimeException("Sender wallet not found"));

        Wallet receiver = walletRepository.findByUserIdForUpdate(toUserId)
                .orElseThrow(() -> new RuntimeException("Receiver wallet not found"));

        // 💸 Check balance
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // ➖ Debit
        sender.setBalance(sender.getBalance().subtract(amount));

        // ➕ Credit
        receiver.setBalance(receiver.getBalance().add(amount));

        walletRepository.save(sender);
        walletRepository.save(receiver);

        // 📒 Ledger entries (VERY IMPORTANT)

        Ledger debitEntry = Ledger.builder()
                .walletId(sender.getId())
                .amount(amount)
                .type(TransactionType.DEBIT)
                .createdAt(LocalDateTime.now())
                .build();

        Ledger creditEntry = Ledger.builder()
                .walletId(receiver.getId())
                .amount(amount)
                .type(TransactionType.CREDIT)
                .createdAt(LocalDateTime.now())
                .build();

        ledgerRepository.save(debitEntry);
        ledgerRepository.save(creditEntry);
    }
}
