package dripnote.lesson.repository;

import dripnote.lesson.domain.ClassReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassReviewRepository extends JpaRepository<ClassReview, Long> {
}
