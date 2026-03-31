package dripnote.lesson.repository;

import dripnote.lesson.domain.ClassSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {
}
