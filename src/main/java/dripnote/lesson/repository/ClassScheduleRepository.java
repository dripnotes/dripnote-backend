package dripnote.lesson.repository;

import dripnote.lesson.entity.ClassScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassScheduleRepository extends JpaRepository<ClassScheduleEntity, Long> {
}
