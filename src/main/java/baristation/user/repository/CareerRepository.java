package baristation.user.repository;

import baristation.user.domain.Career;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CareerRepository extends JpaRepository<Career, Long> {
    List<Career> findByUser_UserId(Long userId);

    /**
     * 벌크 삭제: 특정 사용자의 모든 경력 정보 삭제
     * (회원 탈퇴 시 사용)
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Career c WHERE c.user.userId = :userId")
    void deleteAllByUserIdInQuery(@Param("userId") Long userId);
}
