package dripnote.bean.repository;

import dripnote.bean.domain.BeanImage;
import dripnote.bean.domain.BeanTastingNote;
import dripnote.bean.enums.ImageType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BeanImagesRepository extends JpaRepository<BeanImage, Long> {

    // beanIds에 있는 beanId를 기반으로 지정한 ImageType에 포함되는 이미지 객체만 리스트 반환
    List<BeanImage> findByBean_BeanIdInAndImageType(Collection<Long> beanIds, ImageType imageType);
}
