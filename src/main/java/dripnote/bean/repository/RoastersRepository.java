package dripnote.bean.repository;

import dripnote.bean.entity.RoasterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoastersRepository extends JpaRepository<RoasterEntity, Long> {
}
