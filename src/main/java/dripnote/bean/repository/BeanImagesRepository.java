package dripnote.bean.repository;

import dripnote.bean.domain.BeanImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeanImagesRepository extends JpaRepository<BeanImage, Long> {
}
