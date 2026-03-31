package dripnote.user.entity;

import dripnote.user.enums.UserProvider;
import dripnote.user.enums.UserRole;
import dripnote.user.enums.UserStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
/**
 * 플랫폼마다 provider_id가 겹칠 수 있어서
 * 복합 유니크 제약조건 {provider, provider_id}를 추가
 * -> 플랫폼은 달라도 provider_id가 같을 수 있음
 */
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "unique_provider_id", columnNames = {"provider", "provider_id"})
})
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    // OAuth 정책상 이메일은 선택인 경우가 많아 nullable = true(디폴트)로 변경
    @Column(name = "email", length = 255)
    private String email;

    // OAuth: google, kakao, naver 등
    @Column(name = "provider", nullable = false, length = 20)
    private String provider;

    // 해당 플랫폼에서 제공하는 고유 식별 번호 (sub, id 등)
    @Column(name = "provider_id", nullable = false, length = 255)
    private UserProvider providerId;

    @Column(name = "nickname", nullable = false, length = 50)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /* 네이버/카카오에서 제공하는 성별, 연령대 등 추가 정보를 저장하고 싶을 때 사용 */
    // @Column(name = "gender", length = 10)
    // private String gender;

    // @Column(name = "age_range", length = 20)
    // private String ageRange;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus status = UserStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}