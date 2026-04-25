package com.payment.paymentSystem.service;

import com.payment.paymentSystem.entity.PaymentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentProducer {

    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    public void sendEvent(String topic, PaymentEvent event) {
        kafkaTemplate.send(topic, event.getTransactionId(), event);
    }
}
