package baristation.common.redis;
import baristation.common.annotation.ExternalApiLog;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    @Value("${jwt.refresh_expiration_time}")
    private Duration refreshExp;

    /**
     * RefreshToken 저장
     */
    @ExternalApiLog("Redis - Set Refresh Token")
    public void setRefreshToken(String userId, String refreshToken) {
        redisTemplate.opsForValue().set(
                "RT:" + userId,          // Key (예: RT:user123)
                refreshToken,            // Value
                refreshExp  // 시간, 날짜 기준(Duration)
        );
    }

    /**
     * RefreshToken 조회
     */
    @ExternalApiLog("Redis - Get Refresh Token")
    public String getRefreshToken(String userId) {
        return (String) redisTemplate.opsForValue().get("RT:" + userId);
    }

    /**
     * RefreshToken 삭제
     */
    @ExternalApiLog("Redis - delete Refresh Token")
    public void deleteRefreshToken(String userId) {
        redisTemplate.delete("RT:" + userId);

    }

    /**
     * AccessToken 블랙리스트 등록
     */
    @ExternalApiLog("Redis - set BlackList")
    public void setBlackList(String accessToken, String status, Duration expirationTime) {
        redisTemplate.opsForValue().set(
                accessToken,
                status,
                expirationTime
        );
    }

    /**
     * 블랙리스트 여부 확인
     */
    @ExternalApiLog("Redis - trace BlackList")
    public boolean hasKeyBlackList(String accessToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(accessToken));
    }
}