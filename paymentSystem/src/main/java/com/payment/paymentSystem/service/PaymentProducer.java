package com.payment.paymentSystem.service;

import com.payment.paymentSystem.entity.PaymentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class PaymentProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendEvent(String topic, String key, String payload) throws ExecutionException, InterruptedException {
        kafkaTemplate.send(topic, key, payload).get();
    }
}
