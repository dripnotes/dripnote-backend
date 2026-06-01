# API 에러 관리 가이드 (Common & Matrix)

> **문서 개요**: 서비스 전반에 적용되는 공통 에러 규격과 도메인(기능)별 상세 에러 코드 및 구현 노트를 정의합니다.

---

## 1. 공통 에러 규격 (Common Guide)

모든 API 응답의 에러 객체는 아래 포맷을 따릅니다.

### 공통 에러 응답 포맷
| 필드 | 타입 | 설명 |
|---|---|---|
| **statusCode** | string | 내부 관리용 에러 코드 (`ErrorCode.code`) |
| **message** | string | 사용자 또는 개발자용 메시지 (`ErrorCode.message`) |
| **data** | object \| null | 추가 데이터가 필요한 경우 사용 (기본 `null`) |

### 프론트엔드 분기 권장 기준
1. **`statusCode`**: (최우선) 특정 비즈니스 로직 분기 처리 시 사용
2. **HTTP Status**: 일반적인 에러 카테고리(401, 404, 500 등) 처리 시 사용
3. **`message`**: UI에 텍스트를 그대로 노출할 때만 사용 (코드 분기 조건으로 비권장)

### 글로벌 Fallback 매트릭스
| 처리 지점 | HTTP | statusCode | message | 비고 |
|---|---:|---|---|---|
| `GlobalExceptionHandler` | 400 | `900-3` | 잘못된 요청입니다. | 일반적인 `IllegalArgumentException` |
| `GlobalExceptionHandler` | 500 | `900-4` | 서버 내부 오류가 발생했습니다. | 시스템 예외 (`Runtime`/`Exception`) |

---

## 2. 인증/회원 (Auth/User)

### API 에러코드 매트릭스
| API | HTTP | statusCode | message | 발생 조건 |
|---|---:|---|---|---|
| `POST /api/auth/refresh` | 400 | `701-4` | Refresh Token 헤더는 필수입니다. | 쿠키/헤더의 Refresh Token 누락 또는 공백 |
| `POST /api/auth/refresh` | 401 | `701-1` | 유효하지 않은 토큰입니다. | 토큰 형식/서명 오류 등 |
| `POST /api/auth/refresh` | 401 | `701-2` | 토큰이 만료되었습니다. | Refresh Token 만료 |
| `POST /api/auth/refresh` | 401 | `701-3` | Refresh Token이 일치하지 않거나 만료되었습니다. | Redis에 저장된 토큰과 불일치 |
| `POST /api/auth/refresh` | 404 | `700-6` | 사용자를 찾을 수 없습니다. | 토큰 subject로 사용자 조회 실패 |
| `POST /api/auth/logout` | 401 | `701-1` | 유효하지 않은 토큰입니다. | Access 토큰 누락/형식 오류 등 |
| `PUT /api/auth/update` | 400 | `700-7` | 닉네임은 필수 입력값입니다. | body/nickname null 또는 blank |
| `PUT /api/auth/update` | 400 | `700-8` | 닉네임 형식이 올바르지 않습니다. | 닉네임 검증 실패(길이/문자) |
| `PUT /api/auth/update` | 409 | `700-10` | 이미 사용 중인 닉네임입니다. | 닉네임 중복 처리 |
| `DELETE /api/auth/delete` | 401 | `701-1` | 유효하지 않은 토큰입니다. | Access 토큰 누락/형식 오류 등 |
| `DELETE /api/auth/delete` | 404 | `700-6` | 사용자를 찾을 수 없습니다. | 토큰 subject로 사용자 조회 실패 |
| `DELETE /api/auth/delete` | 409 | `700-13` | 진행 중인 레슨이 있어 회원 탈퇴를 진행할 수 없습니다. | `role=BARISTA` 이고 host_user_id로 연결된 레슨 존재 |
| `DELETE /api/auth/delete` | 500 | `700-12` | 회원 탈퇴 처리 중 오류가 발생했습니다. | 연관 데이터 삭제 중 예상 밖의 서버 오류 |

### 필터/시큐리티 레벨 처리
| 처리 지점 | HTTP | statusCode | message | 비고 |
|---|---:|---|---|---|
| `JwtAuthenticationFilter` | 401 | `701-1` | 유효하지 않은 토큰입니다. | Access 토큰 검증 실패 - 형식/서명 오류 |
| `JwtAuthenticationFilter` | 401 | `701-2` | 토큰이 만료되었습니다. | Access 토큰 만료 |
| `JwtAuthenticationFilter` | 401 | `701-5` | 토큰이 존재하지 않습니다. | 토큰이 헤더에 없음 |
| `JwtAuthenticationFilter` | 401 | `701-6` | 지원되지 않는 토큰입니다. | 토큰 타입 불일치 등 |
| `AccessDecision` / 컨트롤러 보호 | 403 | `701-7` | 접근이 거부되었습니다. | 권한(role) 부족 또는 접근 제약 |

### 구현 노트
- `refresh`는 클라이언트가 보유한 Refresh Token(쿠키 또는 헤더)을 검증하고 Redis의 저장값과 비교합니다. 실패 시 `701-1`/`701-2`/`701-3` 중 적합한 코드를 던집니다.
- `delete` (회원 탈퇴)는 트랜잭션 내에서 자식 데이터(찜, 리뷰, 예약 등) → 부모(회원) 순으로 물리적 삭제를 수행하며, 완료 후 R2 파일 삭제를 비동기로 처리합니다.
- `delete` 요청 시 `BARISTA` 사용자는 본인이 호스팅하는 레슨(`lessons.host_user_id`)이 남아 있으면 `700-13`으로 탈퇴를 거절합니다.
- 인증 실패 시 `CustomAuthenticationEntryPoint`를 통해 클라이언트가 처리 가능한 JSON(401)을 반환하도록 설정되어 있습니다. (리다이렉트 없음)

---

## 3. 홈 (Home)

### API 에러코드 매트릭스
| API | HTTP | statusCode | message | 발생 조건 |
| --- | ---: | --- | --- | --- |
| `GET /api/main` | 404 | `600-1` | 상품 정보가 없습니다. | 추천 상품 리스트 결과가 비어 있음 |

### 서비스 내부 처리 기준
- **`HomeServiceImpl.getProducts()`**: 상품 미존재 시 `CustomException` 발생 (404 / `600-1`)
- **향미 노트 처리**: 홈 목록 조립 시 향미 노트가 없으면 빈 리스트 또는 null을 안전하게 처리하도록 구현

---

## 4. 원두 (Bean)

### API 에러코드 매트릭스
| API | HTTP | statusCode | message | 발생 조건 |
| --- | ---: | --- | --- | --- |
|`GET /api/products/search` | 400 | `600-2` | 검색 조건의 최소값이 최대값보다 클 수 없습니다. | range validation 실패 (min > max) |
|`GET /api/products/search` | 400 | `600-4` | 검색 값은 1 이상 5 이하 여야 합니다. | 검색 점수 값 범위 위반 |
|`GET /api/products/search` | 500 | `600-3` | 원두 검색 중 오류가 발생했습니다. | DB/매핑/페이지 처리 오류 |
|`GET /api/products/{productId}` | 404 | `600-1` | 원두를 찾을 수 없습니다. | BeanProduct/연결 데이터 미존재 |

### 서비스 내부 처리 기준
- **검색 (`BeanService.searchProducts`)**: 입력 범위 검증 실패 시 `600-2` 또는 `600-4` 반환
- **상세 조회 (`BeanService.getProductDetail`)**: 관련 데이터 누락 시 `600-1` (404)
- **응답 조립 시 null-safety**: 이미지/향미가 없으면 null 또는 빈 리스트로 안전 처리 (NPE 방지)

---

## 5. 레슨 (Lesson)

### API 에러코드 매트릭스
| API | HTTP | statusCode | message | 발생 조건 |
| --- | ---: | --- | --- | --- |
| `GET /api/lessons` (검색) | 400 | `601-1` | 클래스 검색 요청이 올바르지 않습니다. | 잘못된 검색 파라미터 |
| `GET /api/lessons` (검색) | 500 | `601-2` | 클래스 검색 중 오류가 발생했습니다. | 내부 처리 오류 |
| `GET /api/lessons/{id}` | 404 | `601-4` | 클래스를 찾을 수 없습니다. | Lesson 미존재 |

### 서비스 내부 처리 기준
- 검색/매핑 단계에서 예외 발생 시 적절한 `CustomException`을 던져 위의 코드로 매핑

---

## 6. 북마크 (Bookmark)

### API 에러코드 매트릭스
| API | HTTP | statusCode | message | 발생 조건 |
| --- | ---: | --- | --- | --- |
| `POST /api/bookmarks/{productId}` | 401 | `701-1` | 유효하지 않은 토큰입니다. | 인증(토큰) 누락 또는 형식 오류 |
| `POST /api/bookmarks/{productId}` | 404 | `600-1` | 원두를 찾을 수 없습니다. | 요청한 productId에 해당하는 상품 미존재 |
| `GET /api/bookmarks/list` | 401 | `701-1` | 유효하지 않은 토큰입니다. | 인증(토큰) 누락 또는 형식 오류 |
| `GET /api/bookmarks/list` | 200 | - | 빈 목록 반환(정상) | 사용자가 북마크한 상품이 없을 때는 빈 페이지 반환 |
| `DELETE /api/auth/delete` (회원탈퇴) | 200 | - | 북마크 데이터 벌크 삭제됨 | 사용자 삭제 시 모든 북마크 데이터 함께 삭제 |

### 서비스 내부 처리 기준
- `toggleBookmark(productId, userId)`
  - 존재하는 북마크면 삭제, 없으면 생성하는 토글 동작을 수행합니다. (트랜잭션 처리)
  - 요청한 `productId`가 존재하지 않으면 `BEAN_NOT_FOUND(600-1)`을 던집니다.
- `getBookmarks(userId, pageable)`
  - 페이지네이션된 결과를 반환하며, 북마크가 없으면 빈 페이지(`Page.empty`)를 반환합니다. 예외가 발생하지 않도록 null-safe하게 이미지/향미 처리를 수행합니다.
- `회원 탈퇴 시` 벌크 DELETE 쿼리로 해당 사용자의 모든 북마크 데이터를 삭제합니다. (N+1 쿼리 방지)

### 구현 노트
- 북마크 관련 엔드포인트는 인증이 필수입니다(`/api/bookmarks/**`은 SecurityConfig에서 `.authenticated()`로 보호됨).
- 클라이언트는 401 응답을 받으면 로그인 플로우를 유도하고, 200 OK의 경우 body에서 content가 빈지 확인하여 처리합니다.

---

## 7. 이미지 (Image)

이미지 업로드/유효성 관련 코드는 `800xx` 영역에 정의되어 있습니다.

### 주요 에러코드
| code | HTTP | message | 발생 조건 |
|---|---:|---|---|
| `800-1` | 400 | 잘못된 imageUrl 형식입니다. | 외부 URL 또는 형식 오류 |
| `800-2` | 400 | 빈 파일은 업로드할 수 없습니다. | Multipart 파일 비어있음 |
| `800-3` | 400 | 지원하지 않는 이미지 형식입니다. | contentType 불허 |
| `800-4` | 400 | 이미지 크기는 5MB 이하여야 합니다. | 파일 사이즈 초과 |
| `800-6` | 404 | 원두 이미지를 찾을 수 없습니다. | 조회 실패 |
| `800-7` | 404 | 클래스를 찾을 수 없습니다. | lesson image 미존재 |
| `800-8` | 400 | 대표 이미지는 전용 API를 사용해주세요. | 썸네일 업데이트 규칙 위반 |
| `800-9` | 500 | 이미지 업로드 중 오류가 발생했습니다. | S3/R2 업로드 실패 등 |

### 구현 노트
- 이미지 처리 로직은 `R2ImageService`를 통해 이루어지며, 업로드 전 content-type/size 검증을 수행합니다.
- PUT(멱등) 방식의 API에서 이미지가 포함되는 경우 기존 objectKey에 덮어쓰기(update)하여 멱등성을 보장하도록 권장합니다.

---

## 8. 토큰/보안 (Token / Security)

토큰 관련 에러는 `701xx` 영역에 정의되어 있으며, 필터/엔트리포인트에서 처리됩니다.

### 주요 에러코드
| code | HTTP | message | 발생 조건 |
|---|---:|---|---|
| `701-1` | 401 | 유효하지 않은 토큰입니다. | 서명/형식 오류 등 |
| `701-2` | 401 | 토큰이 만료되었습니다. | Access/Refresh 토큰 만료 |
| `701-3` | 401 | Refresh Token이 일치하지 않거나 만료되었습니다. | Redis 불일치 |
| `701-4` | 400 | Refresh Token 헤더는 필수입니다. | refresh 요청시 토큰 누락 |
| `701-5` | 401 | 토큰이 존재하지 않습니다. | 토큰 누락 상황 특별 처리 |
| `701-6` | 401 | 지원되지 않는 토큰입니다. | 토큰 타입/알고리즘 불일치 |
| `701-7` | 403 | 접근이 거부되었습니다. | 권한 부족 |

### 구현 노트
- `JwtAuthenticationFilter`는 토큰을 추출/검증 후 `SecurityContext`에 Authentication을 설정합니다. 검증 실패 시 `CustomException`을 던지고 `CustomAuthenticationEntryPoint`를 통해 JSON 응답(401)을 반환합니다.

---

## 9. 동시성 / 기타 (Concurrency & System)

### 주요 에러코드
| code | HTTP | message | 발생 조건 |
|---|---:|---|---|
| `100-1` | 409 | 다른 사용자가 먼저 수정했습니다. 페이지를 새로고침 후 다시 이용해주세요 | 낙관적 락/동시성 충돌 |
| `900-1` | 500 | 암호화 중 오류가 발생했습니다. | AES 암복호화 실패 등 |
| `900-2` | 404 | 지원서가 존재하지 않습니다. | 도메인별 기타 리소스 미존재 |
| `900-3` | 400 | 잘못된 요청입니다. | Global 예외 처리의 기본 매핑 |
| `900-4` | 500 | 서버 내부 오류가 발생했습니다. | Fallback - 시스템 예외 |

### 구현 노트
- 전역 예외 핸들러(`GlobalExceptionHandler`)에서 기본 매핑을 수행하며, 서비스/도메인 레벨에서 던진 `CustomException`의 `ErrorCode`에 따라 HTTP 상태와 body를 구성합니다.
