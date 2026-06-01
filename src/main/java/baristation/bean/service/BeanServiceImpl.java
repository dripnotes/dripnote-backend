package baristation.bean.service;

import baristation.bean.domain.Bean;
import baristation.bean.domain.BeanProduct;
import baristation.bean.domain.Product;
import baristation.bean.domain.ProductImage;
import baristation.bean.enums.ImageType;
import baristation.bean.payload.dto.*;
import baristation.bean.payload.request.ProductSearchRequest;
import baristation.bean.repository.BeanProductRepository;
import baristation.bean.repository.ProductFlavorNoteRepository;
import baristation.bean.repository.ProductImageRepository;
import baristation.bookmark.repository.ProductBookmarkRepository;
import baristation.common.exception.CustomException;
import baristation.common.exception.ErrorCode;
import baristation.common.payload.response.PageResponse;
import baristation.common.r2.ImageUrlResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BeanServiceImpl implements BeanService {

    private final BeanProductRepository beanProductRepository;
    private final ProductFlavorNoteRepository productFlavorNoteRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductBookmarkRepository productBookmarkRepository;
    private final ImageUrlResolver imageUrlResolver;

    /**
     * 조건에 맞는 원두 목록을 검색하고 페이지네이션하여 반환
     * 
     * @param request 검색 필터 조건 (키워드, 맛, 로스팅 정도 등)
     * @param pageable 페이지네이션 정보
     * @return 페이지네이션된 BeanSummaryDTO 응답
     */
    @Override
    public PageResponse<ProductSummaryDTO> searchProducts(ProductSearchRequest request, Pageable pageable) {
        // 조건 검증
        validateSearchRequest(request);

        Page<BeanProduct> beanProductPage = beanProductRepository.searchProductsWithFilters(request, pageable);
        // 검색 조건에 해당하는 상품이 없는 경우
        if (beanProductPage.isEmpty()) {
            return PageResponse.of(Page.empty(pageable));
        }

        List<Long> productIds = beanProductPage.getContent().stream()
                .map(BeanProduct::getProduct)
                .filter(Objects::nonNull)
                .map(Product::getProductId)
                .filter(Objects::nonNull)
                .toList();

        Map<Long, ProductImageDTO> thumbImageByProductId = getThumbImages(productIds);
        Map<Long, FlavorNoteDTO> flavorNoteByProductId = getFlavorNoteMap(productIds);

        Page<ProductSummaryDTO> page = beanProductPage.map(beanProduct -> {
            if (beanProduct.getBean() == null || beanProduct.getProduct() == null || beanProduct.getProduct().getProductId() == null) {
                throw new CustomException(ErrorCode.BEAN_SEARCH_FAILED);
            }
            Long productId = beanProduct.getProduct().getProductId();
            return toSummaryDto(
                    beanProduct.getBean(),
                    productId,
                    thumbImageByProductId.get(productId),
                    flavorNoteByProductId.get(productId)
            );
        });

        return PageResponse.of(page);
    }
    @Override
    public ProductDetailDTO getProductDetail(Long productId, Long userId) {

        BeanProduct selectedBeanProduct = beanProductRepository.findByProductId(productId);

        if (selectedBeanProduct == null || selectedBeanProduct.getBean() == null || selectedBeanProduct.getProduct() == null) {
            throw new CustomException(ErrorCode.BEAN_NOT_FOUND);
        }

        Product product = selectedBeanProduct.getProduct();
        Bean bean = selectedBeanProduct.getBean();
        Long resolvedProductId = product.getProductId();

        // 2. 상세 응답용 전체 이미지와 대표 썸네일을 분리 조회
        List<ProductImageDTO> images = productImageRepository.findByProduct_ProductIdOrderBySortOrderAsc(resolvedProductId)
                .stream()
                .map(this::toProductImageDto)
                .filter(image -> image.imageType() != ImageType.THUMB)
                .toList();

        ProductImageDTO thumbImage = productImageRepository.findByProduct_ProductIdAndImageTypeOrderBySortOrderAsc(
                        resolvedProductId,
                        ImageType.THUMB
                ).stream()
                .findFirst()
                .map(this::toProductImageDto)
                .orElse(null);

        List<FlavorNoteDTO> flavorNotes = getFlavorNotes(resolvedProductId);
        boolean bookmarked = userId != null && productBookmarkRepository.existsByUser_UserIdAndProduct_ProductId(userId, resolvedProductId);

        // 3. 상세 DTO 조립
        ProductSummaryDTO summary = ProductSummaryDTO.builder()
                .productId(productId)
                .beanNameKo(bean.getNameKo())
                .beanNameEn(bean.getNameEn())
                .origin(bean.getOrigin())
                .region(bean.getRegion())
                .process(bean.getProcess())
                .productImage(thumbImage)
                .build();

        return ProductDetailDTO.builder()
                .beanSummary(summary)
                .roaster(RoasterDTO.from(product.getRoaster()))
                .roastingType(product.getRoastLevel())
                .flavorNotes(flavorNotes)
                .description(product.getDescription())
                .productUrl(product.getProductUrl())
                .agtronMin(product.getAgtronMin())
                .agtronMax(product.getAgtronMax())
                .acidity(product.getAcidity())
                .sweetness(product.getSweetness())
                .body(product.getBody())
                .balance(product.getBalance())
                .images(images)
                .bookmarked(bookmarked)
                .build();
    }

    private List<FlavorNoteDTO> getFlavorNotes(Long resolvedProductId) {
        return productFlavorNoteRepository.findByProduct_ProductIdIn(List.of(resolvedProductId))
                .stream()
                .map(productFlavorNote -> FlavorNoteDTO.from(productFlavorNote.getFlavorNote()))
                .toList();
    }

    private Map<Long, ProductImageDTO> getThumbImages(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return productImageRepository.findByProduct_ProductIdInAndImageType(productIds, ImageType.THUMB)
                .stream()
                .collect(Collectors.toMap(
                        image -> image.getProduct().getProductId(),
                        this::toProductImageDto,
                        (first, second) -> first
                ));
    }

    // In을 써서 한번에 조회
    private Map<Long, FlavorNoteDTO> getFlavorNoteMap(List<Long> productIds) {
        if (productIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return productFlavorNoteRepository.findByProduct_ProductIdIn(productIds)
                .stream()
                .collect(Collectors.groupingBy(
                        productFlavorNote -> productFlavorNote.getProduct().getProductId(),
                        Collectors.collectingAndThen(
                                Collectors.minBy(Comparator.comparing(
                                        productFlavorNote -> productFlavorNote.getFlavorNote().getFlavorNoteId(),
                                        Comparator.nullsLast(Long::compareTo)
                                )),
                                flavorNote -> flavorNote
                                        .map(productFlavorNote -> FlavorNoteDTO.from(productFlavorNote.getFlavorNote()))
                                        .orElse(null)
                        )
                ));
    }

    private ProductSummaryDTO toSummaryDto(Bean bean,
                                           Long productId,
                                           ProductImageDTO image,
                                           FlavorNoteDTO flavorNote) {

        // 향미 정보가 없는 경우 null-safe 처리
        FlavorNoteDTO processedFlavorNote = null;
        if (flavorNote != null) {
            processedFlavorNote = flavorNote.toBuilder()
                    .flavorImageUrl(imageUrlResolver.toPublicUrl(flavorNote.flavorImageUrl()))
                    .build();
        }

        return ProductSummaryDTO.builder()
                .productId(productId)
                .beanNameKo(bean.getNameKo())
                .beanNameEn(bean.getNameEn())
                .origin(bean.getOrigin())
                .region(bean.getRegion())
                .process(bean.getProcess())
                .productImage(image)
                .flavorNotes(processedFlavorNote)
                .build();
    }

    private ProductImageDTO toProductImageDto(ProductImage productImage) {
        // 원두 이미지 DB 값은 objectKey이므로 공통 컴포넌트로 public URL을 조립합니다.
        return ProductImageDTO.builder()
                .productImageId(productImage.getProductImageId())
                .imageType(productImage.getImageType())
                .imageUrl(imageUrlResolver.toPublicUrl(productImage.getImageUrl()))
                .sortOrder(productImage.getSortOrder())
                .build();
    }

    private void validateSearchRequest(ProductSearchRequest request) {
        if (request == null) {
            return;
        }

        validateRange(request.minAcidity(), request.maxAcidity());
        validateRange(request.minSweetness(), request.maxSweetness());
        validateRange(request.minBody(), request.maxBody());
        validateRange(request.minBalance(), request.maxBalance());
    }

    private void validateRange(Double min, Double max) {
        if (min == null || max == null || min > max || max > 5 || min < 0)
            throw new CustomException(ErrorCode.BEAN_SEARCH_INVALID_RANGE);
    }

}

