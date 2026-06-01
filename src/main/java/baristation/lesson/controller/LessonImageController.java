package baristation.lesson.controller;

import baristation.common.payload.response.ApiResponse;
import baristation.lesson.payload.response.LessonImageResponse;
import baristation.lesson.service.LessonImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lessons")
public class LessonImageController {

    private final LessonImageService lessonImageService;

    // 클래스 이미지 목록 조회
    @GetMapping("/{lessonId}/images")
    public ResponseEntity<ApiResponse<List<LessonImageResponse>>> getImages(@PathVariable("lessonId") Long lessonId) {
        List<LessonImageResponse> response = lessonImageService.getImages(lessonId);
        return ApiResponse.ok(response);
    }

    // 대표 이미지 업로드 또는 교체
    @PostMapping(value = "/{lessonId}/images/thumb", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LessonImageResponse>> uploadThumb(
            @PathVariable("lessonId") Long lessonId,
            @RequestPart("file") MultipartFile file
    ) {
        LessonImageResponse response = lessonImageService.uploadThumb(lessonId, file);
        return ApiResponse.ok(response);
    }

    // 서브 이미지 추가
    @PostMapping(value = "/{lessonId}/images/sub", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LessonImageResponse>> uploadSub(
            @PathVariable("lessonId") Long lessonId,
            @RequestPart("file") MultipartFile file
    ) {
        LessonImageResponse response = lessonImageService.uploadSub(lessonId, file);
        return ApiResponse.ok(response);
    }

    // 특정 서브 이미지 교체
    @PutMapping(value = "/{lessonId}/images/{lessonImageId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LessonImageResponse>> updateImage(
            @PathVariable("lessonId") Long lessonId,
            @PathVariable("lessonImageId") Long lessonImageId,
            @RequestPart("file") MultipartFile file
    ) {
        LessonImageResponse response = lessonImageService.updateImage(lessonId, lessonImageId, file);
        return ApiResponse.ok(response);
    }

    // 특정 이미지 삭제
    @DeleteMapping("/{lessonId}/images/{lessonImageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable("lessonId") Long lessonId,
            @PathVariable("lessonImageId") Long lessonImageId
    ) {
        lessonImageService.deleteImage(lessonId, lessonImageId);
        return ApiResponse.ok();
    }
}
