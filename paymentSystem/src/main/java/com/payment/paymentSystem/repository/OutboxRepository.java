package com.payment.paymentSystem.repository;

import com.payment.paymentSystem.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxRepository extends JpaRepository<Outbox, Long> {

    List<Outbox> findByPublishedFalse();
}
