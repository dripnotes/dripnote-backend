package dripnote.bean.repository;

import dripnote.bean.domain.BeanTastingNote;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BeanTastingNotesRepository extends JpaRepository<BeanTastingNote, Long> {

    // beanIds에 있는 원두를 조회해서 TastingNote 반환
    // LazyInitializationException이 나지않도록 bean과 tastingNote를 같이 로딩
    @EntityGraph(attributePaths = {"bean", "tastingNote"})
    List<BeanTastingNote> findByBean_BeanIdIn(Collection<Long> beanIds);
}
