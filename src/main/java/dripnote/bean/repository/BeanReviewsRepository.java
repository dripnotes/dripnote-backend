package dripnote.bean.repository;

import dripnote.bean.domain.BeanReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeanReviewsRepository extends JpaRepository<BeanReview, Long> {
}
