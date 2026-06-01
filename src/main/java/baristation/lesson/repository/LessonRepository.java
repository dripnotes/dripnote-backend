package baristation.lesson.repository;

import baristation.lesson.domain.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, Long>, LessonRepositoryCustom {

	boolean existsByHostUser_UserId(Long userId);
}
