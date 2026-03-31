package dripnote.lesson.repository;

import dripnote.lesson.entity.BookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<BookingEntity, Long> {
}
