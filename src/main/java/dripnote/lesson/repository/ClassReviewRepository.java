package dripnote.lesson.repository;

import dripnote.lesson.entity.ClassReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassReviewRepository extends JpaRepository<ClassReviewEntity, Long> {
}
