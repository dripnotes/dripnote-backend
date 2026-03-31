package dripnote.bean.repository;

import dripnote.bean.domain.Roaster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoastersRepository extends JpaRepository<Roaster, Long> {
}
