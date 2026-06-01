package baristation.lesson.repository;

import baristation.lesson.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * 벌크 삭제: 특정 사용자의 모든 예약 정보 삭제
     * (회원 탈퇴 시 사용)
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Booking b WHERE b.user.userId = :userId")
    void deleteAllByUserIdInQuery(@Param("userId") Long userId);
}
