package com.payment.paymentSystem.service;

import com.payment.paymentSystem.entity.*;
import com.payment.paymentSystem.repository.LedgerRepository;
import com.payment.paymentSystem.repository.TransactionRepository;
import com.payment.paymentSystem.repository.WalletRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentConsumer {

    private final WalletService walletService;
    private final LedgerRepository ledgerRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentProducer producer;
    private final ObjectMapper objectMapper;


    @KafkaListener(topics = "payment-topic", groupId = "wallet-group")
    @Transactional
    public void consume(String payload) {

        try {
            PaymentEvent event = objectMapper.readValue(payload, PaymentEvent.class);

            if ("DEBIT".equals(event.getType())) {
                walletService.processDebit(event);
            } else if ("CREDIT".equals(event.getType())) {
                walletService.processCredit(event);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to process Kafka event", e);
        }
    }


}