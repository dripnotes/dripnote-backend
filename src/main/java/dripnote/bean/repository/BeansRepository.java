package dripnote.bean.repository;

import dripnote.bean.domain.Bean;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeansRepository extends JpaRepository<Bean, Long> {
}
