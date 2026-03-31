package dripnote.bean.repository;

import dripnote.bean.entity.BeanBookmarkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BeanBookmarksRepository extends JpaRepository<BeanBookmarkEntity, Long> {
}
