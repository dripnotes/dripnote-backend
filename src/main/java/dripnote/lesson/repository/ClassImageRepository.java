package dripnote.lesson.repository;

import dripnote.lesson.domain.ClassImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassImageRepository extends JpaRepository<ClassImage, Long> {
}
