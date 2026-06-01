package baristation.user.service;

import baristation.bean.domain.Product;
import baristation.bean.domain.ProductBookmark;
import baristation.bean.domain.ProductReview;
import baristation.bean.domain.Roaster;
import baristation.bean.repository.ProductRepository;
import baristation.bean.repository.ProductReviewRepository;
import baristation.bookmark.repository.ProductBookmarkRepository;
import baristation.common.exception.CustomException;
import baristation.common.exception.ErrorCode;
import baristation.common.r2.R2ImageService;
import baristation.common.redis.RedisService;
import baristation.lesson.domain.Lesson;
import baristation.lesson.enums.DifficultyLevel;
import baristation.lesson.enums.LessonCategory;
import baristation.lesson.enums.Region;
import baristation.bean.enums.RoastingType;
import baristation.lesson.repository.LessonRepository;
import baristation.security.jwt.JwtTokenProvider;
import baristation.user.domain.User;
import baristation.user.enums.UserProvider;
import baristation.user.enums.UserRole;
import baristation.user.payload.dto.UserUpdateRequest;
import baristation.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class UserServiceIntegrationTest {

    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private ProductBookmarkRepository productBookmarkRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductReviewRepository productReviewRepository;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private baristation.bean.repository.RoastersRepository roastersRepository;
    @Autowired private EntityManager entityManager;

    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private RedisService redisService;
    @MockitoBean private R2ImageService r2ImageService;

    @AfterEach
    void tearDown() {
        productReviewRepository.deleteAllInBatch();
        productBookmarkRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        lessonRepository.deleteAllInBatch();
        roastersRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("회원 정보 수정: 닉네임과 프로필 이미지를 함께 변경하면 DB에 반영된다")
    void updateUser_updatesNicknameAndProfileImage() throws Exception {
        User user = userRepository.save(User.builder()
                .provider(UserProvider.GOOGLE)
                .providerId("provider-update-1")
                .nickname("원본닉네임")
                .profileImageUrl("/users/1/profile/old.png")
                .role(UserRole.USER)
                .build());

        MockMultipartFile profileImage = new MockMultipartFile(
                "profileImage",
                "profile.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        when(r2ImageService.updateByUrl(any(), eq("/users/1/profile/old.png")))
                .thenReturn("https://pub-test.example.com/users/1/profile/new.png");

        userService.updateUser(user.getUserId(), new UserUpdateRequest("새닉네임"), profileImage);

        User updated = userRepository.getUserByUserId(user.getUserId()).orElseThrow();
        assertThat(updated.getNickname()).isEqualTo("새닉네임");
        assertThat(updated.getProfileImageUrl()).isEqualTo("https://pub-test.example.com/users/1/profile/new.png");
        verify(r2ImageService).updateByUrl(any(), eq("/users/1/profile/old.png"));
    }

    @Test
    @DisplayName("회원 정보 수정: 중복 닉네임이면 변경을 거절한다")
    void updateUser_whenNicknameDuplicate_throwsConflict() {
        userRepository.save(User.builder()
                .provider(UserProvider.GOOGLE)
                .providerId("provider-update-2")
                .nickname("기존닉네임")
                .role(UserRole.USER)
                .build());

        User target = userRepository.save(User.builder()
                .provider(UserProvider.KAKAO)
                .providerId("provider-update-3")
                .nickname("수정대상")
                .role(UserRole.USER)
                .build());

        try {
            userService.updateUser(target.getUserId(), new UserUpdateRequest("기존닉네임"), null);
        } catch (CustomException e) {
            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NICKNAME_DUPLICATE);
            return;
        }
        throw new AssertionError("Expected CustomException to be thrown");
    }

    @Test
    @DisplayName("회원 탈퇴: BARISTA가 호스팅 중인 레슨이 있으면 실제 DB에서도 탈퇴가 거절된다")
    void deleteUser_whenBaristaHasHostedLesson_throwsConflict() {
        User host = userRepository.save(User.builder()
                .provider(UserProvider.GOOGLE)
                .providerId("provider-host-1")
                .nickname("바리스타호스트")
                .role(UserRole.BARISTA)
                .build());

        Lesson lesson = lessonRepository.save(Lesson.builder()
                .hostUser(host)
                .title("V60 클래스")
                .subtitle("테스트")
                .lessonCategory(LessonCategory.HOBBY)
                .difficultyLevel(DifficultyLevel.BEGINNER)
                .region(Region.SEOUL)
                .city("마포구")
                .place("테스트공간")
                .address("서울특별시 마포구")
                .latitude(BigDecimal.valueOf(37.5))
                .longitude(BigDecimal.valueOf(126.9))
                .build());

        when(jwtTokenProvider.getSubject("access-host-token")).thenReturn(String.valueOf(host.getUserId()));

        try {
            userService.deleteUser(mockRequest("access-host-token"));
        } catch (CustomException e) {
            assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_WITHDRAWAL_DENIED_HAS_HOSTED_LESSON);
            assertThat(lessonRepository.findById(lesson.getLessonId())).isPresent();
            assertThat(userRepository.getUserByUserId(host.getUserId())).isPresent();
            return;
        }
        throw new AssertionError("Expected CustomException to be thrown");
    }

    @Test
    @Sql(scripts = "classpath:insert-deleted-user.sql")
    @DisplayName("회원 탈퇴: 일반 사용자는 연관 데이터가 정리되고 리뷰는 user_id=0으로 남는다")
    void deleteUser_success_deletesAndKeepsReviews() {
        User target = userRepository.save(User.builder()
                .provider(UserProvider.KAKAO)
                .providerId("provider-delete-1")
                .nickname("탈퇴대상")
                .profileImageUrl("/users/100/profile.png")
                .role(UserRole.USER)
                .build());

        Roaster roaster = roastersRepository.save(Roaster.builder()
                .nameKo("테스트로스터")
                .nameEn("Test Roaster")
                .homepageUrl("https://example.com")
                .description("test")
                .build());

        Product product = productRepository.save(Product.builder()
                .roaster(roaster)
                .nameKo("테스트원두")
                .nameEn("Test Bean")
                .roastLevel(RoastingType.LIGHT)
                .acidity(3.0)
                .sweetness(3.0)
                .body(3.0)
                .balance(3.0)
                .description("test")
                .build());

        ProductBookmark bookmark = productBookmarkRepository.save(ProductBookmark.builder()
                .user(target)
                .product(product)
                .build());

        ProductReview review = productReviewRepository.save(ProductReview.builder()
                .user(target)
                .product(product)
                .content("좋아요")
                .build());

        when(jwtTokenProvider.getSubject("access-delete-token")).thenReturn(String.valueOf(target.getUserId()));
        when(jwtTokenProvider.getExpirationToken("access-delete-token")).thenReturn(3_600_000L);
        when(redisService.getRefreshToken(String.valueOf(target.getUserId()))).thenReturn(null);

        userService.deleteUser(mockRequest("access-delete-token"));

        assertThat(userRepository.getUserByUserId(target.getUserId())).isEmpty();
        assertThat(productBookmarkRepository.findById(bookmark.getBookmarkId())).isEmpty();

        Long reviewUserId = getProductReviewUserId(review.getProductReviewId());
        assertThat(reviewUserId).isZero();

        verify(redisService).deleteRefreshToken(String.valueOf(target.getUserId()));
        verify(redisService).setBlackList("access-delete-token", "delete", Duration.ofMillis(3_600_000L));
        verify(r2ImageService, timeout(3000)).deleteByUrl("/users/100/profile.png");
    }

    private Long getProductReviewUserId(Long reviewId) {
        return entityManager.createQuery(
                        "select pr.user.userId from ProductReview pr where pr.productReviewId = :reviewId",
                        Long.class)
                .setParameter("reviewId", reviewId)
                .getSingleResult();
    }

    private jakarta.servlet.http.HttpServletRequest mockRequest(String accessToken) {
        jakarta.servlet.http.HttpServletRequest request = org.mockito.Mockito.mock(jakarta.servlet.http.HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + accessToken);
        return request;
    }
}








