package com.example.Dripnote.lesson.repository;

import com.example.Dripnote.lesson.entity.ClassScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassScheduleRepository extends JpaRepository<ClassScheduleEntity, Long> {
}
