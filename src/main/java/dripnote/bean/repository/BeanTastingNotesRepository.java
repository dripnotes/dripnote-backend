package dripnote.bean.repository;

import dripnote.bean.entity.BeanTastingNoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BeanTastingNotesRepository extends JpaRepository<BeanTastingNoteEntity, Long> {
}
