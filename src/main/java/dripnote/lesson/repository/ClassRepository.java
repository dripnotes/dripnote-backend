package dripnote.lesson.repository;

import dripnote.lesson.domain.Class;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassRepository extends JpaRepository<Class, Long> {
}
