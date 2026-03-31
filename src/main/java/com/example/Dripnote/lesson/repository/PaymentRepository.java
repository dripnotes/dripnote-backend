package com.example.Dripnote.lesson.repository;

import com.example.Dripnote.lesson.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
}
