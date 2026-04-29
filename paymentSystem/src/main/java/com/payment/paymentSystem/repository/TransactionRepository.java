package com.payment.paymentSystem.repository;

import com.payment.paymentSystem.entity.Transaction;
import com.payment.paymentSystem.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {
    List<Transaction> findByStatusAndCreatedAtBefore(TransactionStatus list, LocalDateTime time);
}