package baristation.bean.controller;

import baristation.bean.payload.dto.ProductDetailDTO;
import baristation.bean.payload.dto.ProductSummaryDTO;
import baristation.bean.payload.request.ProductSearchRequest;
import baristation.bean.service.BeanService;
import baristation.common.payload.response.ApiResponse;
import baristation.common.payload.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {
    private final BeanService beanService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<ProductSummaryDTO>>> searchProducts(
            @ModelAttribute ProductSearchRequest request,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PageResponse<ProductSummaryDTO> response = beanService.searchProducts(request, pageable);
        return ApiResponse.ok(response);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<ProductDetailDTO>> getProductDetail(@PathVariable Long productId) {
        ProductDetailDTO response = beanService.getProductDetail(productId);
        return ApiResponse.ok(response);
    }
}
