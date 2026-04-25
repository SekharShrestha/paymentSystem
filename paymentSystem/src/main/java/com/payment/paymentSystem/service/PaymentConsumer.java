package com.payment.paymentSystem.service;

import com.payment.paymentSystem.entity.*;
import com.payment.paymentSystem.repository.LedgerRepository;
import com.payment.paymentSystem.repository.TransactionRepository;
import com.payment.paymentSystem.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentConsumer {

    private final WalletRepository walletRepository;
    private final LedgerRepository ledgerRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentProducer producer;

    @KafkaListener(topics = "payment-topic", groupId = "wallet-group")
    @Transactional
    public void handleDebit(PaymentEvent event) {

        if (!event.getType().equals("DEBIT")) return;

        Transaction txn = transactionRepository.findById(event.getTransactionId())
                .orElseThrow();

        if (txn.getStatus() != TransactionStatus.INITIATED) return;

        Wallet sender = walletRepository.findByUserIdForUpdate(event.getFromUserId())
                .orElseThrow();

        if (sender.getBalance().compareTo(event.getAmount()) < 0) {
            txn.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(txn);
            return;
        }

        // idempotent check
        boolean alreadyDebited = ledgerRepository
                .existsByTransactionIdAndType(event.getTransactionId(), TransactionType.DEBIT);

        if (!alreadyDebited) {
            sender.setBalance(sender.getBalance().subtract(event.getAmount()));
            walletRepository.save(sender);

            ledgerRepository.save(Ledger.builder()
                    .walletId(sender.getId())
                    .transactionId(event.getTransactionId())
                    .amount(event.getAmount())
                    .type(TransactionType.DEBIT)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        txn.setStatus(TransactionStatus.DEBIT_DONE);
        transactionRepository.save(txn);

        // 🔥 publish credit event
        producer.sendEvent("payment-topic", PaymentEvent.builder()
                .transactionId(event.getTransactionId())
                .fromUserId(event.getFromUserId())
                .toUserId(event.getToUserId())
                .amount(event.getAmount())
                .type("CREDIT")
                .build());
    }

    @KafkaListener(topics = "payment-topic", groupId = "wallet-group")
    @Transactional
    public void handleCredit(PaymentEvent event) {

        if (!event.getType().equals("CREDIT")) return;

        Transaction txn = transactionRepository.findById(event.getTransactionId())
                .orElseThrow();

        if (txn.getStatus() != TransactionStatus.DEBIT_DONE) return;

        Wallet receiver = walletRepository.findByUserIdForUpdate(event.getToUserId())
                .orElseThrow();

        boolean alreadyCredited = ledgerRepository
                .existsByTransactionIdAndType(event.getTransactionId(), TransactionType.CREDIT);

        if (!alreadyCredited) {
            receiver.setBalance(receiver.getBalance().add(event.getAmount()));
            walletRepository.save(receiver);

            ledgerRepository.save(Ledger.builder()
                    .walletId(receiver.getId())
                    .transactionId(event.getTransactionId())
                    .amount(event.getAmount())
                    .type(TransactionType.CREDIT)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        txn.setStatus(TransactionStatus.SUCCESS);
        transactionRepository.save(txn);
    }
}
