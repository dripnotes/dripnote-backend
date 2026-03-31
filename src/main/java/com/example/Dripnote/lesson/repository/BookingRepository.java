package com.example.Dripnote.lesson.repository;

import com.example.Dripnote.lesson.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
}
