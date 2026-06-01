package baristation.bookmark.controller;

import baristation.bean.payload.dto.ProductSummaryDTO;
import baristation.bookmark.service.BookmarkService;
import baristation.common.payload.response.ApiResponse;
import baristation.common.payload.response.PageResponse;
import baristation.security.annotation.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {
    private final BookmarkService bookmarkService;
    // 북마크 토글(추가/삭제)
    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<Void>> toggleBookmark(
            @PathVariable Long productId,
            @CurrentUserId Long userId
    ) {
        bookmarkService.toggleBookmark(productId, userId);
        return ApiResponse.ok(null);
    }
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryDTO>>> getBookmarks(
            @CurrentUserId Long userId,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        PageResponse<ProductSummaryDTO> response = bookmarkService.getBookmarks(userId, pageable);
        return ApiResponse.ok(response);
    }
}
