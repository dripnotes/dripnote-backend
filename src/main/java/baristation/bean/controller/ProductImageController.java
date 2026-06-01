package baristation.bean.controller;

import baristation.bean.payload.response.BeanImageResponse;
import baristation.bean.service.BeanImageServiceImpl;
import baristation.common.payload.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/beans")
public class ProductImageController {

    private final BeanImageServiceImpl beanImageService;

    // 원두 이미지 목록 조회
    @GetMapping("/{productId}/images")
    public ResponseEntity<ApiResponse<List<BeanImageResponse>>> getImages(@PathVariable("productId") Long productId) {
        List<BeanImageResponse> response = beanImageService.getImages(productId);
        return ApiResponse.ok(response);
    }

    // 대표 이미지 업로드 또는 교체
    @PostMapping(value = "/{productId}/images/thumb", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BeanImageResponse>> uploadThumb(
            @PathVariable("productId") Long productId,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        BeanImageResponse response = beanImageService.uploadThumb(productId, file);
        return ApiResponse.ok(response);
    }

    // 서브 이미지 추가
    @PostMapping(value = "/{productId}/images/sub", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BeanImageResponse>> uploadSub(
            @PathVariable("productId") Long productId,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        BeanImageResponse response = beanImageService.uploadSub(productId, file);
        return ApiResponse.ok(response);
    }

    // 특정 서브 이미지 교체
    @PutMapping(value = "/{productId}/images/{beanImageId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BeanImageResponse>> updateImage(
            @PathVariable("productId") Long productId,
            @PathVariable("beanImageId") Long beanImageId,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        BeanImageResponse response = beanImageService.updateImage(beanImageId, file);
        return ApiResponse.ok(response);
    }

    // 특정 이미지 삭제
    @DeleteMapping("/{productId}/images/{beanImageId}")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable("productId") Long productId,
            @PathVariable("beanImageId") Long beanImageId
    ) {
        beanImageService.deleteImage(beanImageId);
        return ApiResponse.ok();
    }
}