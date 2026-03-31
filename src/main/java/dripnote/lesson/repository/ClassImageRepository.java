package dripnote.lesson.repository;

import dripnote.lesson.entity.ClassImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassImageRepository extends JpaRepository<ClassImageEntity, Long> {
}
