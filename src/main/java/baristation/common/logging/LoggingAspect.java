package baristation.common.logging;

import baristation.common.annotation.ExternalApiLog;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.slf4j.MDC;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    // @ExternalApiLog 어노테이션이 붙은 메서드를 타겟으로 설정
    @Around("@annotation(externalApiLog)")
    public Object logExternalApiExecution(ProceedingJoinPoint jp, ExternalApiLog externalApiLog) throws Throwable {
        String className = jp.getTarget().getClass().getSimpleName();
        String methodName = jp.getSignature().getName();
        String traceId = TraceIdUtil.getTraceId();
        boolean mdcSetByAspect = false;

        // MDC에 traceId가 없으면(예: @Async로 다른 스레드에서 실행될 때),
        // 메서드 인자에서 traceId를 추출해 MDC에 설정 시도
        if (traceId == null || "-".equals(traceId)) {
            String extracted = extractTraceIdFromArgs(jp.getArgs());
            if (extracted != null && !extracted.isBlank() && !"-".equals(extracted)) {
                MDC.put(TraceIdUtil.TRACE_ID_KEY, extracted);
                mdcSetByAspect = true;
                traceId = extracted;
            }
        }

        // 어노테이션에 적어둔 설명값 가져오기 (없으면 빈 문자열)
        String apiDescription = externalApiLog.value().isEmpty() ? "" : " [" + externalApiLog.value() + "]";

        long startTime = System.currentTimeMillis();
        try {
            Object result = jp.proceed();
            long duration = System.currentTimeMillis() - startTime;

            // 2. 외부 API 요청 성공 및 소요 시간 로그
            log.info("[External API Response]{} {} -> {} done. duration={}ms, traceId={}",
                    apiDescription, className, methodName, duration, traceId);

            return result;

        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;

            // 3. 외부 API 통신 중 에러 발생 시 소요 시간과 함께 경고 로그 기록
            log.warn("[External API Failed]{} {} -> {} exception={}! duration={}ms, traceId={}",
                    apiDescription, className, methodName, e.getClass().getSimpleName(), duration, traceId);
            throw e;
        } finally {
            if (mdcSetByAspect) {
                MDC.remove(TraceIdUtil.TRACE_ID_KEY);
            }
        }
    }

    // 메서드 인자에서 traceId를 추출 (event.record 타입 등에서 traceId() 메서드를 호출)
    private String extractTraceIdFromArgs(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg == null) continue;
            try {
                var method = arg.getClass().getMethod("traceId");
                if (method != null) {
                    Object val = method.invoke(arg);
                    if (val instanceof String) return (String) val;
                }
            } catch (NoSuchMethodException ignored) {
                // 무시
            } catch (Exception ignored) {
                // 호출 실패 시 무시
            }
        }
        return null;
    }
}