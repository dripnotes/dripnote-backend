package dripnote.user.domain;

import dripnote.user.enums.UserProvider;
import dripnote.user.enums.UserRole;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// 세터 삭제했어요
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor // builder때문에 필요
@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "unique_provider_id", columnNames = {"provider", "provider_id"})
})
public class User {
    /**
     * updatedAt, profileImageUrl, status -> 해당 데이터는 현재 불필요할 것 같아서 삭제했습니다!
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    // Kakao, Naver, Google 전부 이메일을 제공해서 null false로 수정하였습니다.
    @Column(name = "email", nullable = false, length = 255)
    private String email;

    // OAuth: google, kakao, naver 등
    @Enumerated(EnumType.STRING) // ENUM
    @Column(name = "provider", nullable = false, length = 20)
    private UserProvider provider;

    // 해당 플랫폼에서 제공하는 고유 식별 번호 (sub, id 등)
    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    // 일단
    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    /* 네이버/카카오에서 제공하는 성별, 연령대 등 추가 정보를 저장하고 싶을 때 사용 */
    // @Column(name = "gender", length = 10)
    // private String gender;

    // @Column(name = "age_range", length = 20)
    // private String ageRange;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void updateNickname(String newNickname) {
        if (newNickname == null || newNickname.isBlank()) {
            throw new IllegalArgumentException("닉네임은 필수 입력값입니다.");
        }

        // 추후 중복여부 글자수 제한 등 로직 추가
        this.nickname = newNickname;
    }

}