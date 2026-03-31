package dripnote.bean.repository;

import dripnote.bean.domain.BeanTastingNote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeanTastingNotesRepository extends JpaRepository<BeanTastingNote, Long> {
}
