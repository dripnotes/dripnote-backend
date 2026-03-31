package dripnote.lesson.repository;

import dripnote.lesson.entity.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassRepository extends JpaRepository<ClassEntity, Long> {
}
