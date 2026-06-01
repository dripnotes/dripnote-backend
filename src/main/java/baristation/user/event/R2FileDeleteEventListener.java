package baristation.user.event;

import baristation.common.r2.R2ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.List;

/**
 * 회원 탈퇴 시 R2 스토리지의 파일 삭제를 처리하는 비동기 이벤트 리스너
 *
 * 주의사항:
 * - DB 트랜잭션이 정상 커밋된 AFTER_COMMIT 단계에서만 실행됨
 * - @Async를 통해 별도의 스레드에서 비동기로 처리되어 응답 지연 방지
 * - 파일 삭제 실패 시 로그만 기록되고 DB 롤백에는 영향을 주지 않음
 */
@Component
@RequiredArgsConstructor
public class R2FileDeleteEventListener {

    private final R2ImageService r2ImageService;

    /**
     * 회원 삭제 이벤트 처리
     *
     * @param event 삭제 대상 회원의 파일 키 목록을 담은 이벤트
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void handleUserDeletedEvent(UserDeletedEvent event) {
        List<String> fileKeys = event.r2FileKeys();
        if (fileKeys.isEmpty()) return;

        boolean hasError = false;
        StringBuilder errorSummary = new StringBuilder();

        for (String fileKey : fileKeys) {
            if (fileKey == null || fileKey.isEmpty()) continue;

            try {
                r2ImageService.deleteByUrl(fileKey);
            } catch (Exception e) {
                hasError = true;
                errorSummary.append(String.format("[fileKey=%s, error=%s] ", fileKey, e.getMessage()));
                // 💡 여기서 throw 하지 않음으로써, 하나의 파일이 실패해도 다음 파일 삭제를 계속 진행합니다!
            }
        }

        // 💡 모든 파일 순회가 끝난 후, 하나라도 실패가 있었다면 예외를 발생시켜 AOP가 캐치하게 만듭니다.
        if (hasError) {
            throw new RuntimeException("R2 파일 삭제 중 일부 실패 발생. userId=" + event.userId() + " -> " + errorSummary);
        }
    }
}

