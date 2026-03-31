package dripnote.bean.repository;

import dripnote.bean.entity.BeanEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeansRepository extends JpaRepository<BeanEntity, Long> {
}
