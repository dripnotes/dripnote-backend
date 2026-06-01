package baristation.bookmark.service;

import baristation.bean.domain.*;
import baristation.bean.enums.ImageType;
import baristation.bean.payload.dto.FlavorNoteDTO;
import baristation.bean.payload.dto.ProductImageDTO;
import baristation.bean.payload.dto.ProductSummaryDTO;
import baristation.bean.repository.BeanProductRepository;
import baristation.bean.repository.ProductFlavorNoteRepository;
import baristation.bean.repository.ProductImageRepository;
import baristation.bookmark.repository.ProductBookmarkRepository;
import baristation.bean.repository.ProductRepository;
import baristation.common.exception.CustomException;
import baristation.common.exception.ErrorCode;
import baristation.common.payload.response.PageResponse;
import baristation.common.r2.ImageUrlResolver;
import baristation.user.domain.User;
import baristation.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookmarkServiceImpl implements BookmarkService {

    private final ProductBookmarkRepository productBookmarkRepository;
    private final ProductRepository productRepository;
    private final BeanProductRepository beanProductRepository;
    private final UserRepository userRepository;
    private final ImageUrlResolver imageUrlResolver;
    private final ProductFlavorNoteRepository productFlavorNoteRepository;
    private final ProductImageRepository productImageRepository;

    @Override
    @Transactional
    public void toggleBookmark(Long productId, Long userId) {
        User userProxy = userRepository.getReferenceById(userId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.BEAN_NOT_FOUND));

        // 이미 존재하면 삭제
        productBookmarkRepository.findByUserAndProduct(userProxy, product)
                .ifPresentOrElse(
                        bookmark ->
                            productBookmarkRepository.delete(bookmark),
                        () -> {
                            ProductBookmark newBookmark = ProductBookmark
                                    .builder()
                                    .user(userProxy)
                                    .product(product)
                                    .build();
                            productBookmarkRepository.save(newBookmark);
                        }
                );
    }

    @Override
    public PageResponse<ProductSummaryDTO> getBookmarks(Long userId, Pageable pageable) {

        // userId가 북마크한 상품 조회 (BeanProductRepository의 커스텀 메소드 사용)
        Page<BeanProduct> beanProductPage = beanProductRepository.searchProductsWithUserId(pageable, userId);
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
}

