package com.payment.paymentSystem.config;

import com.payment.paymentSystem.entity.Outbox;
import com.payment.paymentSystem.entity.PaymentEvent;
import com.payment.paymentSystem.entity.Transaction;
import com.payment.paymentSystem.entity.TransactionStatus;
import com.payment.paymentSystem.repository.LedgerRepository;
import com.payment.paymentSystem.repository.OutboxRepository;
import com.payment.paymentSystem.repository.TransactionRepository;
import com.payment.paymentSystem.repository.WalletRepository;
import com.payment.paymentSystem.service.PaymentProducer;
import com.payment.paymentSystem.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class Schedulers {


    private final TransactionRepository transactionRepository;
    private final PaymentProducer producer;
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    public void retryPendingTransactions() {

        List<Transaction> txns = transactionRepository
                .findByStatusAndCreatedAtBefore(
                        TransactionStatus.INITIATED,
//                        TransactionStatus.DEBIT_DONE
                        LocalDateTime.now().minusMinutes(5));

        for (Transaction txn : txns) {

            try {
                // 🔥 recreate DEBIT event
                PaymentEvent event = PaymentEvent.builder()
                        .transactionId(txn.getIdempotencyKey())
                        .fromUserId(txn.getFromUserId())
                        .toUserId(txn.getToUserId())
                        .amount(txn.getAmount())
                        .type("DEBIT")
                        .build();

                // 🔥 send again to Kafka
                producer.sendEvent(
                        "payment-topic",
                        txn.getIdempotencyKey(),
                        objectMapper.writeValueAsString(event)
                );

                System.out.println("Recovered txn: " + txn.getIdempotencyKey());

            } catch (Exception e) {
                System.out.println("Recovery failed for txn: " + txn.getIdempotencyKey());
            }

        }
    }

    @Scheduled(fixedDelay = 2000) // every 2 seconds
    public void publish() {

        List<Outbox> events = outboxRepository.findByPublishedFalse();

        for (Outbox e : events) {
            try {
                kafkaTemplate.send("payment-topic", e.getAggregateId(), e.getPayload()).get();

                e.setPublished(true);
                outboxRepository.save(e);

            } catch (Exception ex) {
                // log and retry later
                System.out.println("Failed to publish event: " + e.getId());
            }
        }
    }
}
