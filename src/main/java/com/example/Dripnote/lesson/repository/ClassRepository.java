package com.example.Dripnote.lesson.repository;

import com.example.Dripnote.lesson.entity.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassRepository extends JpaRepository<ClassEntity, Long> {
}
