package baristation.lesson.controller;

import baristation.common.payload.response.ApiResponse;
import baristation.common.payload.response.PageResponse;
import baristation.lesson.payload.dto.LessonDTO;
import baristation.lesson.payload.dto.LessonDetailDTO;
import baristation.lesson.payload.request.LessonSearchRequest;
import baristation.lesson.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/lessons")
public class LessonController {
    private final LessonService lessonService;

    /**
     * 클래스 검색 요청 파라미터와 페이징 정보를 받아 검색 결과를 반환한다.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<LessonDTO>>> searchLessons(
            @ModelAttribute LessonSearchRequest request,
            @PageableDefault(size = 12) Pageable pageable
    ) {
        PageResponse<LessonDTO> response = lessonService.searchLessons(request, pageable);
        return ApiResponse.ok(response);
    }

    /**
     * 클래스 상세 정보를 조회한다. 상세 구현은 LessonService 구현 상태를 따른다.
     */
    @GetMapping("/{lessonId}")
    public ResponseEntity<ApiResponse<LessonDetailDTO>> getLessonDetail(@PathVariable Long lessonId) {
        LessonDetailDTO response = lessonService.getLessonDetail(lessonId);
        return ApiResponse.ok(response);
    }
}