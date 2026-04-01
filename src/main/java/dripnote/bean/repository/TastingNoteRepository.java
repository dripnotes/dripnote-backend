package dripnote.bean.repository;

import dripnote.bean.domain.TastingNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TastingNoteRepository extends JpaRepository<TastingNote, Long> {
    // ID 기준으로 오름차순 정렬 후 4개 반환
    List<TastingNote> findTop4ByOrderByTastingNoteIdAsc();
}
