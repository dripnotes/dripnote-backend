package baristation.user.repository;

import baristation.user.domain.User;
import baristation.user.enums.UserProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Provider별로 ProviderId를 통해 사용자 조회
    Optional<User> findByProviderAndProviderId(UserProvider provider, String providerId);

    boolean existsByNickname(String nickname);

    /**
     * 대소문자를 무시하고 닉네임 중복 여부 확인
     * (예: "SeongWoo"와 "seongwoo"는 동일인으로 간주)
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE LOWER(u.nickname) = LOWER(:nickname)")
    boolean existsByNicknameIgnoreCase(@Param("nickname") String nickname);

    Optional<User> getUserByUserId(Long userId);

    /**
     * 사용자의 프로필 이미지 키 목록 조회
     * (회원 탈퇴 시 R2 파일 삭제용)
     */
    @Query("SELECT u.profileImageUrl FROM User u WHERE u.userId = :userId AND u.profileImageUrl IS NOT NULL")
    List<String> findProfileImageKeysByUserId(@Param("userId") Long userId);
}
