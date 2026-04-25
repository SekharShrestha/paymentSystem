package com.payment.paymentSystem.service;

import com.payment.paymentSystem.dto.TransferRequest;
import com.payment.paymentSystem.entity.*;
import com.payment.paymentSystem.repository.LedgerRepository;
import com.payment.paymentSystem.repository.TransactionRepository;
import com.payment.paymentSystem.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentProducer producer;
    private final OutboxRepository outboxRepository;

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

        Transaction txn = transactionRepository.findById(idempotencyKey).orElse(null);

        if (txn == null) {
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

            // 🔒 Ordered locking
            Long first = Math.min(fromUserId, toUserId);
            Long second = Math.max(fromUserId, toUserId);

            Wallet firstWallet = walletRepository.findByUserIdForUpdate(first)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));

            Wallet secondWallet = walletRepository.findByUserIdForUpdate(second)
                    .orElseThrow(() -> new RuntimeException("Wallet not found"));

            Wallet sender = fromUserId.equals(first) ? firstWallet : secondWallet;
            Wallet receiver = toUserId.equals(first) ? firstWallet : secondWallet;

            // =========================
            // 🧠 STEP 1: DEBIT (IDEMPOTENT)
            // =========================

            if (txn.getStatus() == TransactionStatus.INITIATED) {

                boolean alreadyDebited = ledgerRepository
                        .existsByTransactionIdAndType(idempotencyKey, TransactionType.DEBIT);

                if (!alreadyDebited) {

                    if (sender.getBalance().compareTo(amount) < 0) {
                        throw new RuntimeException("Insufficient balance");
                    }

                    sender.setBalance(sender.getBalance().subtract(amount));
                    walletRepository.save(sender);

                    Ledger debit = Ledger.builder()
                            .walletId(sender.getId())
                            .transactionId(idempotencyKey)
                            .amount(amount)
                            .type(TransactionType.DEBIT)
                            .createdAt(LocalDateTime.now())
                            .build();

                    ledgerRepository.save(debit);
                }

                txn.setStatus(TransactionStatus.DEBIT_DONE);
                transactionRepository.save(txn);
            }

            // =========================
            // 🧠 STEP 2: CREDIT (IDEMPOTENT)
            // =========================

            if (txn.getStatus() == TransactionStatus.DEBIT_DONE) {

                boolean alreadyCredited = ledgerRepository
                        .existsByTransactionIdAndType(idempotencyKey, TransactionType.CREDIT);

                if (!alreadyCredited) {

                    receiver.setBalance(receiver.getBalance().add(amount));
                    walletRepository.save(receiver);

                    Ledger credit = Ledger.builder()
                            .walletId(receiver.getId())
                            .transactionId(idempotencyKey)
                            .amount(amount)
                            .type(TransactionType.CREDIT)
                            .createdAt(LocalDateTime.now())
                            .build();

                    ledgerRepository.save(credit);
                }

                txn.setStatus(TransactionStatus.CREDIT_DONE);
                transactionRepository.save(txn);
            }

            // =========================
            // 🧠 FINAL STEP
            // =========================

            txn.setStatus(TransactionStatus.SUCCESS);
            transactionRepository.save(txn);

        } catch (Exception e) {

            txn.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(txn);

            throw e;
        }
    }

    @Transactional
    public void initiateTransaction(String key, TransferRequest req) {

        if (transactionRepository.existsById(key)) return;

        Transaction txn = Transaction.builder()
                .idempotencyKey(key)
                .fromUserId(req.getFromUserId())
                .toUserId(req.getToUserId())
                .amount(req.getAmount())
                .status(TransactionStatus.INITIATED)
                .build();

        transactionRepository.save(txn);

        // 🔥 publish debit event
        producer.sendEvent("payment-topic", PaymentEvent.builder()
                .transactionId(key)
                .fromUserId(req.getFromUserId())
                .toUserId(req.getToUserId())
                .amount(req.getAmount())
                .type("DEBIT")
                .build());

        // 3️⃣ Save OUTBOX (same transaction)
        Outbox outbox = Outbox.builder()
                .aggregateId(key)
                .type("DEBIT")
                .payload(convertToJson(event))
                .published(false)
                .createdAt(LocalDateTime.now())
                .build();

        outboxRepository.save(outbox);
    }

    @Scheduled(fixedDelay = 5000)
    public void retryPendingTransactions() {

        List<Transaction> txns = transactionRepository
                .findByStatusIn(List.of(
                        TransactionStatus.INITIATED,
                        TransactionStatus.DEBIT_DONE
                ));

        for (Transaction txn : txns) {

            try {
                transferMoney(
                        txn.getIdempotencyKey(),
                        txn.getFromUserId(),
                        txn.getToUserId(),
                        txn.getAmount()
                );
            } catch (Exception ignored) {
            }
        }
    }
}
