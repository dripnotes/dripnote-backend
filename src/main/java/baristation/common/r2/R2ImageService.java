package baristation.common.r2;

import baristation.common.annotation.ExternalApiLog;
import baristation.common.exception.CustomException;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import static baristation.common.exception.ErrorCode.*;

@Service
@ConditionalOnProperty(prefix = "app.r2", name = "enabled", havingValue = "true", matchIfMissing = true)
public class R2ImageService {

    // 허용할 이미지 타입
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    // 최대 업로드 크기: 5MB
    private static final long MAX_SIZE = 5L * 1024 * 1024;

    private final S3Client s3Client;
    private final R2Properties r2Properties;
    private final ImageUrlResolver imageUrlResolver;

    public R2ImageService(S3Client s3Client, R2Properties r2Properties, ImageUrlResolver imageUrlResolver) {
        this.s3Client = s3Client;
        this.r2Properties = r2Properties;
        this.imageUrlResolver = imageUrlResolver;
    }

    /**
     * 원두 대표(THUMB) 이미지 업로드
     * 저장 경로: beans/{productId}/thumb.webp
     */
    @ExternalApiLog("R2 원두 Thumb 이미지 업로드")
    public String uploadBeanThumb(MultipartFile file, Long beanId) throws IOException {
        // 원두 이미지는 DB에 전체 URL이 아니라 objectKey 형태로 저장합니다.
        return uploadFixedFile(file, buildBeanFolder(beanId), "thumb");
    }

    /**
     * 원두 서브(SUB) 이미지 업로드
     * 저장 경로: beans/{productId}/sub_{uuid}.webp
     */
    @ExternalApiLog("R2 원두 Sub 이미지 업로드")
    public String uploadBeanSubImage(MultipartFile file, Long beanId) throws IOException {
        // 원두 이미지는 DB에 public URL prefix 없이 path만 저장합니다.
        return uploadUniqueFile(file, buildBeanFolder(beanId), "sub");
    }

    /**
     * 레슨 대표(THUMB) 이미지 업로드
     * 저장 경로: lessons/{lessonId}/thumb.webp
     */
    @ExternalApiLog("R2 레슨 Thumb 이미지 업로드")
    public String uploadLessonThumb(MultipartFile file, Long lessonId) throws IOException {
        // 레슨 이미지는 향미 이미지와 동일하게 DB에 path/objectKey만 저장합니다.
        return uploadFixedFile(file, buildLessonFolder(lessonId), "thumb");
    }

    /**
     * 레슨 서브(SUB) 이미지 업로드
     * 저장 경로: lessons/{lessonId}/sub_{uuid}.webp
     */
    @ExternalApiLog("R2 레슨 Sub 이미지 업로드")
    public String uploadLessonSubImage(MultipartFile file, Long lessonId) throws IOException {
        // 레슨 이미지는 저장 시 objectKey만 남기고, 응답 서비스에서 public URL prefix를 붙입니다.
        return uploadUniqueFile(file, buildLessonFolder(lessonId), "sub");
    }

    /**
     * 프로필 이미지 업로드
     * 최종 저장 경로: users/{userId}/profile.{확장자}
     */
    @ExternalApiLog("R2 프로필 이미지 업로드")
    public String uploadProfileImage(MultipartFile file, Long userId) throws IOException {
        // 프로필 이미지는 저장 시 objectKey만 남기고, 응답 서비스에서 public URL prefix를 붙입니다.
        return uploadUniqueFile(file, buildUserFolder(userId), "profile");
    }

    // 기존 objectKey 위치의 파일 업데이트
    @ExternalApiLog("R2 이미지 objectKey 기준 업데이트")
    public String updateByObjectKey(MultipartFile file, String objectKey) throws IOException {
        validate(file);
        putObject(file, objectKey);
        return buildPublicUrl(objectKey);
    }

    // imageUrl에서 objectKey를 추출한 뒤 해당 위치의 파일 업데이트
    @ExternalApiLog("R2 이미지 url 기준 업데이트")
    public String updateByUrl(MultipartFile file, String imageUrl) throws IOException {
        String objectKey = extractObjectKey(imageUrl);
        return updateByObjectKey(file, objectKey);
    }

    // imageUrl 기준으로 파일 삭제
    @ExternalApiLog("R2 이미지 url 기준 삭제")
    public void deleteByUrl(String imageUrl) {
        String objectKey = extractObjectKey(imageUrl);
        deleteByObjectKey(objectKey);
    }

    // objectKey 기준으로 파일 삭제
    public void deleteByObjectKey(String objectKey) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(r2Properties.bucketName())
                .key(objectKey)
                .build();

        s3Client.deleteObject(request);
    }

    /**
     * 공개 URL에서 objectKey만 추출
     * 예: https://.../beans/1/thumb.webp -> beans/1/thumb.webp
     */
    public String extractObjectKey(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new CustomException(INVALID_IMAGE_URL);
        }

        if (imageUrlResolver.isExternalUrl(imageUrl)) {
            throw new CustomException(INVALID_IMAGE_URL);
        }

        String objectKey = imageUrlResolver.toObjectKey(imageUrl);
        return objectKey;
    }

    // objectKey를 공개 URL로 변환
    public String buildPublicUrl(String objectKey) {
        String publicUrl = imageUrlResolver.toPublicUrl(objectKey);
        return publicUrl;
    }

    /**
     * 고정 파일명 업로드
     * 저장 구조: {folderPath}/{imageType}/{fileName}.{extension}
     * 예시: beans/1/thumb/thumbnail.jpg
     */
    private String uploadFixedFile(MultipartFile file, String folderPath, String imageType) throws IOException {
        validate(file);

        String extension = extractExtension(file.getOriginalFilename());
        String objectKey = buildObjectKey(folderPath, imageType, imageType, extension);

        putObject(file, objectKey);
        // 원두/레슨 이미지 row에는 전체 URL이 아니라 objectKey만 저장되도록 반환합니다.
        return objectKey;
    }

    /**
     * UUID 기반 파일명 업로드
     * 저장 구조: {folderPath}/{imageType}/{fileName}.{extension}
     * 예시: beans/1/sub/sub_550e8400-e29b-41d4-a716-446655440000.jpg
     */
    private String uploadUniqueFile(MultipartFile file, String folderPath, String imageType) throws IOException {
        validate(file);

        String extension = extractExtension(file.getOriginalFilename());
        String objectKey = buildObjectKey(folderPath, imageType, imageType + "_" + UUID.randomUUID(), extension);

        putObject(file, objectKey);
        // 원두/레슨 이미지 row에는 전체 URL이 아니라 objectKey만 저장되도록 반환합니다.
        return objectKey;
    }

    private String buildObjectKey(String folderPath, String imageType, String fileName, String extension) {
        return folderPath + "/" + imageType + "/" + fileName + "." + extension;
    }

    // R2에 파일 업로드
    private void putObject(MultipartFile file, String objectKey) throws IOException {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(r2Properties.bucketName())
                .key(objectKey)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
    }

    /**
     * 원두 이미지 폴더 경로 생성
     * 예: beans/product-image/3
     */
    private String buildBeanFolder(Long beanId) {
        return "beans/product-image/" + beanId;
    }

    /**
     * 클래스 이미지 폴더 경로 생성
     * 예: lessons/5
     */
    private String buildLessonFolder(Long lessonId) {
        return "lessons/" + lessonId;
    }

    /**
     * 유저 전용 폴더 경로 생성
     * 예: users/5
     */
    private String buildUserFolder(Long userId) {
        return "users/" + userId;
    }

    // 업로드 가능한 파일인지 검증
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(EMPTY_IMAGE_FILE);
        }

        if (!ALLOWED_CONTENT_TYPES.contains(file.getContentType())) {
            throw new CustomException(UNSUPPORTED_IMAGE_TYPE);
        }

        if (file.getSize() > MAX_SIZE) {
            throw new CustomException(IMAGE_SIZE_EXCEEDED);
        }
    }

    // 원본 파일에서 확장자 추출, 없으면 기본 확장자로 webp 사용
    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "webp";
        }

        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
    }
}
