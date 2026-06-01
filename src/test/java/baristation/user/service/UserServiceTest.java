package baristation.user.service;

import baristation.common.exception.CustomException;
import baristation.common.exception.ErrorCode;
import baristation.common.redis.RedisService;
import baristation.lesson.repository.LessonRepository;
import baristation.lesson.repository.LessonReviewRepository;
import baristation.security.jwt.JwtTokenProvider;
import baristation.user.domain.User;
import baristation.user.enums.UserProvider;
import baristation.user.enums.UserRole;
import baristation.user.event.UserDeletedEvent;
import baristation.user.repository.CareerRepository;
import baristation.user.repository.UserRepository;
import baristation.bookmark.repository.ProductBookmarkRepository;
import baristation.bean.repository.ProductReviewRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CareerRepository careerRepository;
    @Mock private ProductBookmarkRepository productBookmarkRepository;
    @Mock private ProductReviewRepository productReviewRepository;
    @Mock private LessonReviewRepository lessonReviewRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RedisService redisService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원 탈퇴: BARISTA가 호스팅 중인 레슨이 있으면 탈퇴를 거절한다")
    void deleteUser_whenBaristaHasHostedLesson_throwsConflict() {
        long userId = 10L;
        String accessToken = "access-token";
        User user = User.builder()
                .userId(userId)
                .provider(UserProvider.GOOGLE)
                .providerId("provider-id")
                .nickname("바리스타")
                .role(UserRole.BARISTA)
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + accessToken);
        when(jwtTokenProvider.getSubject(accessToken)).thenReturn(String.valueOf(userId));
        when(userRepository.getUserByUserId(userId)).thenReturn(Optional.of(user));
        when(lessonRepository.existsByHostUser_UserId(userId)).thenReturn(true);

        assertThatThrownBy(() -> userService.deleteUser(request))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.USER_WITHDRAWAL_DENIED_HAS_HOSTED_LESSON);

        verify(careerRepository, never()).deleteAllByUserIdInQuery(any());
        verify(productBookmarkRepository, never()).deleteAllByUserIdInQuery(any());
        verify(productReviewRepository, never()).updateUserIdToDeletedByUserIdInQuery(any());
        verify(lessonReviewRepository, never()).updateUserIdToDeletedByUserIdInQuery(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("회원 탈퇴: 일반 사용자는 연관 데이터 정리 후 이벤트를 발행한다")
    void deleteUser_success_deletesAndPublishesEvent() {
        long userId = 20L;
        String accessToken = "access-token-2";
        User user = User.builder()
                .userId(userId)
                .provider(UserProvider.KAKAO)
                .providerId("provider-id-2")
                .nickname("일반회원")
                .profileImageUrl("/users/20/profile.png")
                .role(UserRole.USER)
                .build();
        List<String> fileKeys = List.of("/users/20/profile.png", "/users/20/banner.png");

        when(request.getHeader("Authorization")).thenReturn("Bearer " + accessToken);
        when(jwtTokenProvider.getSubject(accessToken)).thenReturn(String.valueOf(userId));
        when(jwtTokenProvider.getExpirationToken(accessToken)).thenReturn(3_600_000L);
        when(userRepository.getUserByUserId(userId)).thenReturn(Optional.of(user));
        when(userRepository.findProfileImageKeysByUserId(userId)).thenReturn(fileKeys);

        userService.deleteUser(request);

        verify(careerRepository).deleteAllByUserIdInQuery(userId);
        verify(productBookmarkRepository).deleteAllByUserIdInQuery(userId);
        verify(productReviewRepository).updateUserIdToDeletedByUserIdInQuery(userId);
        verify(lessonReviewRepository).updateUserIdToDeletedByUserIdInQuery(userId);
        verify(userRepository).delete(user);
        verify(redisService).deleteRefreshToken(String.valueOf(userId));
        verify(redisService).setBlackList(accessToken, "delete", Duration.ofMillis(3_600_000L));

        ArgumentCaptor<UserDeletedEvent> eventCaptor = ArgumentCaptor.forClass(UserDeletedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().userId()).isEqualTo(userId);
        assertThat(eventCaptor.getValue().r2FileKeys()).containsExactlyElementsOf(fileKeys);
    }
}

