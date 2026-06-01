package baristation.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    CONCURRENCY_CONFLICT(HttpStatus.CONFLICT, "100-1","다른 사용자가 먼저 수정했습니다. 페이지를 새로고침 후 다시 이용해주세요"),

    // 600xx: Bean 관련 오류
    BEAN_NOT_FOUND(HttpStatus.NOT_FOUND, "600-1", "원두를 찾을 수 없습니다."),
    BEAN_SEARCH_INVALID_RANGE(HttpStatus.BAD_REQUEST, "600-2", "검색 조건의 최소값이 최대값보다 클 수 없습니다."), // 600-2 최성우: 잘못된 검색조건 추가.
    BEAN_SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "600-3", "원두 검색 중 오류가 발생했습니다."), // 600-3 최성우: 검색 시 DB 오류 추가
    BEAN_SEARCH_INVALID_VALUE(HttpStatus.BAD_REQUEST, "600-4", "검색 값은 1 이상 5 이하 여야 합니다."), // 600-4 최성우: 검색조건의 값이 1~5 사이가 아닐 때 추가

    CLUB_SEARCH_FAILED(HttpStatus.BAD_REQUEST, "600-5", "검색 중 오류가 발생했습니다."),
    CLUB_DIVISION_INVALID(HttpStatus.BAD_REQUEST, "600-6", "올바르지 않은 분과입니다."),
    CLUB_CATEGORY_INVALID(HttpStatus.BAD_REQUEST, "600-7", "올바르지 않은 분류입니다."),
    TOO_MANY_TAGS(HttpStatus.BAD_REQUEST, "600-8", "태그는 최대 3개까지 입력할 수 있습니다."),
    TOO_LONG_TAG(HttpStatus.BAD_REQUEST, "600-9", "태그는 최대 5글자까지 입력할 수 있습니다."),
    TOO_LONG_INTRODUCTION(HttpStatus.BAD_REQUEST, "600-10", "소개는 최대 24글자까지 입력할 수 있습니다."),
    CLUB_NAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "600-11", "이미 사용 중인 동아리 이름입니다."),

    // 601xx: Lesson 관련 오류
    LESSON_SEARCH_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "601-1", "클래스 검색 요청이 올바르지 않습니다."), // 601-1 이형동: 잘못된 검색 요청
    LESSON_SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "601-2", "클래스 검색 중 오류가 발생했습니다."), // 601-2 이형동: 검색 처리 중 예상 밖의 서버 오류 (서버 로그 확인)
    LESSON_SEARCH_MAPPING_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "601-3", "클래스 검색 결과를 응답으로 변환할 수 없습니다."), // 601-3 이형동: lesson이 없거나 hostUser가 없을 때
    LESSON_NOT_FOUND(HttpStatus.NOT_FOUND, "601-4", "클래스를 찾을 수 없습니다."), // 601-4 이형동: lesson이 없을 때

    // 700xx: 사용자/권한 관련 오류
    USER_INVALID_LOGIN(HttpStatus.BAD_REQUEST, "700-4", "올바르지 않은 로그인입니다."),
    USER_UNAUTHORIZED(HttpStatus.FORBIDDEN, "700-5", "권한이 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "700-6", "사용자를 찾을 수 없습니다."),
    USER_NICKNAME_REQUIRED(HttpStatus.BAD_REQUEST, "700-7", "닉네임은 필수 입력값입니다."),
    USER_NICKNAME_INVALID_FORMAT(HttpStatus.BAD_REQUEST, "700-8", "닉네임은 2~20자의 한글, 영문, 숫자, 언더바(_), 대시(-)만 사용 가능합니다."),
    USER_NICKNAME_RESERVED(HttpStatus.BAD_REQUEST, "700-9", "사용할 수 없는 예약어입니다."),
    USER_NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "700-10", "이미 사용 중인 닉네임입니다."),
    USER_NICKNAME_INVALID_SPECIAL_CHAR(HttpStatus.BAD_REQUEST, "700-11", "특수문자가 연속되거나 시작/끝에 위치할 수 없습니다."),
    USER_WITHDRAWAL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "700-12", "회원 탈퇴 처리 중 오류가 발생했습니다."), // 회원 탈퇴 중 예상 밖의 서버 오류
    USER_WITHDRAWAL_DENIED_HAS_HOSTED_LESSON(HttpStatus.CONFLICT, "700-13", "진행 중인 레슨이 있어 회원 탈퇴를 진행할 수 없습니다."),

    // 701xx: 토큰 관련 오류
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "701-1", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "701-2", "토큰이 만료되었습니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "701-3", "Refresh Token이 일치하지 않거나 만료되었습니다."),
    REFRESH_TOKEN_REQUIRED(HttpStatus.BAD_REQUEST, "701-4", "Refresh Token 헤더는 필수입니다."),
    TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "701-5", "토큰이 존재하지 않습니다."), // 701-5 최성우: 토큰이 존재하지 않을 때 추가
    UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "701-6", "지원되지 않는 토큰입니다."), // 701-6 최성우: 지원되지 않는 토큰일 때 추가
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "701-7", "접근이 거부되었습니다."), // 701-7 최성우: 접근이 거부되었을 때 추가

    // 800xx: 이미지 관련
    INVALID_IMAGE_URL(HttpStatus.BAD_REQUEST, "800-1", "잘못된 imageUrl 형식입니다."),
    EMPTY_IMAGE_FILE(HttpStatus.BAD_REQUEST, "800-2", "빈 파일은 업로드할 수 없습니다."),
    UNSUPPORTED_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "800-3", "지원하지 않는 이미지 형식입니다."),
    IMAGE_SIZE_EXCEEDED(HttpStatus.BAD_REQUEST, "800-4", "이미지 크기는 5MB 이하여야 합니다."),
    BEAN_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "800-6", "원두 이미지를 찾을 수 없습니다."),
    LESSON_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "800-7", "클래스 이미지를 찾을 수 없습니다."), // 800-7 이형동: lesson image가 없을 때
    THUMB_IMAGE_UPDATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "800-8", "대표 이미지는 전용 API를 사용해주세요."),
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "800-9", "이미지 업로드 중 오류가 발생했습니다."), // 800-9 이형동: 이미지 업로드 중 불미스러운 사고

    // 900xx: 기타 시스템 오류
    AES_CIPHER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "900-1", "암호화 중 오류가 발생했습니다."),
    APPLICANT_NOT_FOUND(HttpStatus.NOT_FOUND, "900-2", "지원서가 존재하지 않습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "900-3", "잘못된 요청입니다."),
    COMMON_INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "900-4", "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
