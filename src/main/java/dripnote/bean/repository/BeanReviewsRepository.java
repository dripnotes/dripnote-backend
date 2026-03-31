package dripnote.bean.repository;

import dripnote.bean.entity.BeanReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeanReviewsRepository extends JpaRepository<BeanReviewEntity, Long> {
}
