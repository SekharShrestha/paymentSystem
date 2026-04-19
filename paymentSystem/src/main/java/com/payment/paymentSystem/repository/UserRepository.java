package com.payment.paymentSystem.repository;


import com.payment.paymentSystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
