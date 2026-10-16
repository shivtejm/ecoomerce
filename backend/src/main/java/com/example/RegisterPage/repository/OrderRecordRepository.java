package com.example.RegisterPage.repository;

import com.example.RegisterPage.entity.OrderRecord;
import com.example.RegisterPage.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRecordRepository extends JpaRepository<OrderRecord, Long> {
    List<OrderRecord> findByUserOrderByCreatedAtDesc(User user);
    Optional<OrderRecord> findByRazorpayOrderId(String razorpayOrderId);
}
