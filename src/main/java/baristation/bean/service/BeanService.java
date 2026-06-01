package baristation.bean.service;

import baristation.common.payload.response.PageResponse;
import baristation.bean.payload.request.ProductSearchRequest;
import baristation.bean.payload.dto.ProductDetailDTO;
import baristation.bean.payload.dto.ProductSummaryDTO;
import org.springframework.data.domain.Pageable;

public interface BeanService {
    /**
     * 조건에 맞는 원두 목록을 검색
     */
    PageResponse<ProductSummaryDTO> searchProducts(ProductSearchRequest request, Pageable pageable);
    
    /**
     * 원두 단건 상세 조회
     */
    ProductDetailDTO getProductDetail(Long productId, Long userId);
}