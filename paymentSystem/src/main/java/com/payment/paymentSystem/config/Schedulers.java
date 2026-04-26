//package com.payment.paymentSystem.config;
//
//import com.payment.paymentSystem.entity.Outbox;
//import com.payment.paymentSystem.entity.Transaction;
//import com.payment.paymentSystem.entity.TransactionStatus;
//import com.payment.paymentSystem.repository.LedgerRepository;
//import com.payment.paymentSystem.repository.OutboxRepository;
//import com.payment.paymentSystem.repository.TransactionRepository;
//import com.payment.paymentSystem.repository.WalletRepository;
//import com.payment.paymentSystem.service.PaymentProducer;
//import com.payment.paymentSystem.service.WalletService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import org.springframework.stereotype.Service;
//import tools.jackson.databind.ObjectMapper;
//
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//public class Schedulers {
//
//
//    private final TransactionRepository transactionRepository;
//    private final WalletService walletService;
//    private final OutboxRepository outboxRepository;
//    private final KafkaTemplate<String, String> kafkaTemplate;
//
//    @Scheduled(fixedDelay = 5000)
//    public void retryPendingTransactions() {
//
//        List<Transaction> txns = transactionRepository
//                .findByStatusIn(List.of(
//                        TransactionStatus.INITIATED,
//                        TransactionStatus.DEBIT_DONE
//                ));
//
//        for (Transaction txn : txns) {
//
//            try {
//                walletService.transferMoney(
//                        txn.getIdempotencyKey(),
//                        txn.getFromUserId(),
//                        txn.getToUserId(),
//                        txn.getAmount()
//                );
//            } catch (Exception ignored) {
//            }
//        }
//    }
//
//    @Scheduled(fixedDelay = 2000) // every 2 seconds
//    public void publish() {
//
//        List<Outbox> events = outboxRepository.findByPublishedFalse();
//
//        for (Outbox e : events) {
//            try {
//                kafkaTemplate.send("payment-topic", e.getAggregateId(), e.getPayload());
//
//                e.setPublished(true);
//                outboxRepository.save(e);
//
//            } catch (Exception ex) {
//                // log and retry later
//                System.out.println("Failed to publish event: " + e.getId());
//            }
//        }
//    }
//}
