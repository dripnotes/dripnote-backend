package baristation.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 외부 API 통신 메서드의
 * 실행 시간 및 추적을 위한 로깅 어노테이션
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExternalApiLog {
    String value() default ""; // 어떤 외부 API인지 설명을 적을 수 있는 옵션 (예: "Kakao OAuth", "R2 Upload")
}