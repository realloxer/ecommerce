package com.example.myapp.repository;

import com.example.myapp.entity.RefundRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RefundRecordRepository extends JpaRepository<RefundRecord, UUID> {
    List<RefundRecord> findByOrderId(UUID orderId);
}