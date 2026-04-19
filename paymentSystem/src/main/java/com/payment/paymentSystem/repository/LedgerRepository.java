package com.payment.paymentSystem.repository;


import com.payment.paymentSystem.entity.Ledger;
import com.payment.paymentSystem.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepository extends JpaRepository<Ledger, Long> {
    boolean existsByTransactionIdAndType(String transactionId, TransactionType type);
}
