package com.payment.paymentSystem.repository;


import com.payment.paymentSystem.entity.Ledger;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LedgerRepository extends JpaRepository<Ledger, Long> {
}
