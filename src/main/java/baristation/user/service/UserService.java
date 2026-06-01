package baristation.user.service;

import baristation.bean.repository.ProductReviewRepository;
import baristation.bookmark.repository.ProductBookmarkRepository;
import baristation.common.exception.CustomException;
import baristation.common.exception.ErrorCode;
import baristation.common.logging.TraceIdUtil;
import baristation.common.r2.R2ImageService;
import baristation.common.redis.RedisService;
import baristation.lesson.repository.BookingRepository;
import baristation.lesson.repository.LessonRepository;
import baristation.lesson.repository.LessonReviewRepository;
import baristation.security.jwt.JwtTokenProvider;
import baristation.security.payload.dto.TokenPair;
import baristation.user.event.UserDeletedEvent;
import baristation.user.enums.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import baristation.user.domain.User;
import baristation.user.payload.dto.UserUpdateRequest;
import baristation.user.repository.CareerRepository;
import baristation.user.repository.UserRepository;
import baristation.user.validator.NicknameValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final CareerRepository careerRepository;
    private final ProductBookmarkRepository productBookmarkRepository;
    private final ProductReviewRepository productReviewRepository;
    private final BookingRepository bookingRepository;
    private final LessonReviewRepository lessonReviewRepository;
    private final LessonRepository lessonRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;
    private final R2ImageService r2ImageService;
    private final NicknameValidator nicknameValidator;
    private final ApplicationEventPublisher eventPublisher;

    public void logout(HttpServletRequest request) {
        String accessToken = resolveToken(request);
        Long userId = extractUserId(accessToken);
        String userIdText = String.valueOf(userId);

        redisService.deleteRefreshToken(userIdText);
        long expiration = jwtTokenProvider.getExpirationToken(accessToken);
        redisService.setBlackList(accessToken, "logout", Duration.ofMillis(expiration));

    }

    public TokenPair refresh(String refreshToken) {
        // null/blank 체크
        if (!StringUtils.hasText(refreshToken)) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_REQUIRED);
        }

        // validateRefreshToken은 CustomException을 던지므로 catch 불필요
        jwtTokenProvider.validateRefreshToken(refreshToken);

        Long userId = extractUserId(refreshToken);
        String userIdText = String.valueOf(userId);


        String savedUserRefreshToken = redisService.getRefreshToken(userIdText);
        if (savedUserRefreshToken == null || !savedUserRefreshToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }
        User user = userRepository.getUserByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        TokenPair newTokenPair = jwtTokenProvider.createTokenSet(user);
        redisService.setRefreshToken(userIdText, newTokenPair.refreshToken());
        return newTokenPair;
    }

    /**
     * 회원 탈퇴 처리
     *
     * 프로세스:
     * 1. 회원 정보 조회 및 프로필 이미지 경로 추출 (R2 파일 삭제용)
     * 2. 연관된 모든 자식 데이터 벌크 삭제 (N+1 쿼리 방지)
     *    - BARISTA 사용자가 호스팅 중인 레슨이 있으면 탈퇴 거절
     *    - Career: 경력 정보
     *    - ProductBookmark: 찜한 상품
     *    - ProductReview: 상품 리뷰
     *    - Booking: 레슨 예약
     *    - LessonReview: 레슨 리뷰
     * 3. 회원 데이터 삭제
     * 4. Redis에서 Refresh Token 삭제
     * 5. 현재 Access Token 블랙리스트 처리
     * 6. R2 파일 삭제 이벤트 발행 (DB 커밋 후 비동기로 처리)
     *
     * @param request HTTP 요청 (Authorization 헤더에서 토큰 추출)
     */
    public void deleteUser(HttpServletRequest request) {
        String accessToken = resolveToken(request);
        Long userId = extractUserId(accessToken);
        String userIdText = String.valueOf(userId);

        User user = userRepository.getUserByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (user.getRole() == UserRole.BARISTA && lessonRepository.existsByHostUser_UserId(userId)) {
            throw new CustomException(ErrorCode.USER_WITHDRAWAL_DENIED_HAS_HOSTED_LESSON);
        }

        // 1. 회원의 R2 파일 경로 조회 (삭제 전에 반드시 조회)
        List<String> fileKeysToDelete = userRepository.findProfileImageKeysByUserId(userId);

        // 2. 자식 데이터 벌크 삭제 (외래키 참조 순서 주의: 자식 -> 부모)
        careerRepository.deleteAllByUserIdInQuery(userId);
        productBookmarkRepository.deleteAllByUserIdInQuery(userId);
        
        // 상품 리뷰와 레슨 리뷰는 user_id를 0으로 덮어씌우기 (데이터 보존)
        productReviewRepository.updateUserIdToDeletedByUserIdInQuery(userId);
        lessonReviewRepository.updateUserIdToDeletedByUserIdInQuery(userId);

        // 결제의 경우 -> 회원탈퇴시 로그에 기록 - 추후 구현
//        bookingRepository.deleteAllByUserIdInQuery(userId);

        // 3. 회원(부모) 데이터 삭제
        userRepository.delete(user);

        // 4. Redis에서 Refresh Token 삭제
        redisService.deleteRefreshToken(userIdText);

        // 5. 현재 Access Token 블랙리스트 처리
        long expiration = jwtTokenProvider.getExpirationToken(accessToken);
        redisService.setBlackList(accessToken, "delete", Duration.ofMillis(expiration));

        // 6. R2 파일 삭제 이벤트 발행
        // (DB 트랜잭션 커밋 후 AFTER_COMMIT 리스너가 비동기로 처리)
        eventPublisher.publishEvent(new UserDeletedEvent(userId, fileKeysToDelete, TraceIdUtil.getTraceId()));

        log.info("[Auth] User withdrawal completed. userId={}, fileCount={}, traceId={}",
                userId, fileKeysToDelete.size(), TraceIdUtil.getTraceId());
    }

    /**
     * 회원 정보 수정 (닉네임, 이미지)
     * - 닉네임 검증 (형식, 금지어, 특수문자)
     * - 중복 체크 (대소문자 무시)
     * - 프로필 이미지 처리
     */
    public void updateUser(Long userId, UserUpdateRequest updateRequest, MultipartFile profileImage) {
        User user = userRepository.getUserByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (updateRequest == null || !StringUtils.hasText(updateRequest.nickname())) {
            throw new CustomException(ErrorCode.USER_NICKNAME_REQUIRED);
        }

        String newNickname = updateRequest.nickname();

        // 1. 닉네임 형식 검증 (길이, 허용 문자, 금지어, 특수문자)
        nicknameValidator.validate(newNickname);

        // 2. 중복 체크 (대소문자 무시, 자신의 현재 닉네임 제외)
        if (!user.getNickname().equalsIgnoreCase(newNickname)
                && userRepository.existsByNicknameIgnoreCase(newNickname)) {
            throw new CustomException(ErrorCode.USER_NICKNAME_DUPLICATE);
        }

        // 3. 검증 완료된 닉네임 업데이트
        user.updateNickname(newNickname);

        if (profileImage != null && !profileImage.isEmpty()) {
            // 1. 형식 검증
            String contentType = profileImage.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new CustomException(ErrorCode.UNSUPPORTED_IMAGE_TYPE);
            }

            try {
                String oldImageUrl = user.getProfileImageUrl();
                String newImageUrl;

                if (oldImageUrl != null && !oldImageUrl.isEmpty()) {
                    // 기존 이미지가 있으면 해당 위치에 덮어쓰기(update)하여 멱등성 보장
                    newImageUrl = r2ImageService.updateByUrl(profileImage, oldImageUrl);
                } else {
                    // 기존 이미지가 없으면 새로 업로드
                    newImageUrl = r2ImageService.uploadProfileImage(profileImage, userId);
                }

                user.updateProfileImageUrl(newImageUrl);
            } catch (IOException e) {
                String traceId = TraceIdUtil.getTraceId();
                log.error("프로필 이미지 업로드 실패. traceId={}, userId={}", traceId, userId, e);
                throw new CustomException(ErrorCode.IMAGE_UPLOAD_FAILED);
            }
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new CustomException(ErrorCode.TOKEN_INVALID);
    }

    private Long extractUserId(String token) {
        String subject = jwtTokenProvider.getSubject(token);

        if (!StringUtils.hasText(subject)) {
            throw new CustomException(ErrorCode.TOKEN_INVALID);
        }

        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            throw new CustomException(ErrorCode.TOKEN_INVALID);
        }
    }
}
