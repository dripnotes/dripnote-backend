package com.example.Dripnote.bean.repository;

import com.example.Dripnote.bean.entity.BeanTastingNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeanTastingNotesRepository extends JpaRepository<BeanTastingNoteEntity, Long> {
}
