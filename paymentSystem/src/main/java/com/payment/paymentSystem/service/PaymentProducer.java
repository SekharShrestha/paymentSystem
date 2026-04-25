package com.payment.paymentSystem.service;

import com.payment.paymentSystem.entity.PaymentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendEvent(String topic, String key, String payload) {
        kafkaTemplate.send(topic, key, payload);
    }
}
