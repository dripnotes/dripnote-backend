package dripnote.bean.repository;

import dripnote.bean.entity.BeanImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeanImagesRepository extends JpaRepository<BeanImageEntity, Long> {
}
