package baristation.user.event;

import java.util.List;

/**
 * 회원 탈퇴 시 발행되는 이벤트
 * DB 트랜잭션이 완료된 후 비동기로 R2 파일 삭제 처리를 위해 사용됩니다.
 */
public record UserDeletedEvent(
    Long userId,
    List<String> r2FileKeys, // 삭제해야 할 R2 파일의 키(경로) 목록
    String traceId
) {

    public UserDeletedEvent {
        r2FileKeys = r2FileKeys == null ? List.of() : List.copyOf(r2FileKeys);
    }
}

