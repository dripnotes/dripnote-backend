package baristation.bean.service;

import baristation.bean.domain.Bean;
import baristation.bean.domain.BeanProduct;
import baristation.bean.domain.FlavorNote;
import baristation.bean.domain.Product;
import baristation.bean.domain.ProductFlavorNote;
import baristation.bean.domain.ProductImage;
import baristation.bean.domain.Roaster;
import baristation.bean.enums.BeanSortType;
import baristation.bean.enums.FlavorCategory;
import baristation.bean.enums.ImageType;
import baristation.bean.enums.RoastingType;
import baristation.bean.payload.dto.ProductDetailDTO;
import baristation.bean.payload.dto.ProductImageDTO;
import baristation.bean.payload.dto.ProductSummaryDTO;
import baristation.bean.payload.request.ProductSearchRequest;
import baristation.bean.repository.BeanProductRepository;
import baristation.bean.repository.ProductFlavorNoteRepository;
import baristation.bean.repository.ProductImageRepository;
import baristation.bookmark.repository.ProductBookmarkRepository;
import baristation.common.exception.CustomException;
import baristation.common.exception.ErrorCode;
import baristation.common.payload.response.PageResponse;
import baristation.common.r2.ImageUrlResolver;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BeanServiceImplTest {

    @Mock
    private BeanProductRepository beanProductRepository;

    @Mock
    private ProductFlavorNoteRepository productFlavorNoteRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    @Mock
    private ProductBookmarkRepository productBookmarkRepository;

    @Mock
    private ImageUrlResolver imageUrlResolver;

    @InjectMocks
    private BeanServiceImpl beanService;

    @Test
    @DisplayName("원두 목록 조회: DB 없이 mock 엔티티로 페이지/썸네일 매핑을 검증한다")
    void searchProducts_withMockEntities_returnsPagedSummary() {
        when(imageUrlResolver.toPublicUrl(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Pageable pageable = PageRequest.of(0, 12);
        ProductSearchRequest request = new ProductSearchRequest(
                "ethiopia", FlavorCategory.CHOCOLATY,
                2.0, 4.0,
                4.0, 4.0,
                1.0, 1.0,
                5.0, 5.0,
                RoastingType.LIGHT, BeanSortType.ACIDITY
        );

        Bean bean1 = Bean.builder()
                .nameKo("에티오피아 구지")
                .nameEn("Ethiopia Guji")
                .process("워시드")
                .origin("Ethiopia")
                .region("Guji")
                .build();

        Bean bean2 = Bean.builder()
                .nameKo("케냐 AA")
                .nameEn("Keynya AA")
                .process("내츄럴")
                .origin("Kenya")
                .region("Nyeri")
                .build();

        Product product1 = Product.builder()
                .productId(101L)
                .roastLevel(RoastingType.LIGHT)
                .acidity(4.0)
                .sweetness(3.0)
                .body(2.0)
                .balance(3.0)
                .build();

        Product product2 = Product.builder()
                .productId(102L)
                .roastLevel(RoastingType.MEDIUM)
                .acidity(3.0)
                .sweetness(4.0)
                .body(3.0)
                .balance(4.0)
                .build();

        // mock으로 대체 -> 원두와 상품 가져옴
        BeanProduct beanProduct1 = mock(BeanProduct.class);
        when(beanProduct1.getBean()).thenReturn(bean1);
        when(beanProduct1.getProduct()).thenReturn(product1);

        // 원두와 상품 2
        BeanProduct beanProduct2 = mock(BeanProduct.class);
        when(beanProduct2.getBean()).thenReturn(bean2);
        when(beanProduct2.getProduct()).thenReturn(product2);

        Page<BeanProduct> page = new PageImpl<>(List.of(beanProduct1, beanProduct2), pageable, 2);
        when(beanProductRepository.searchProductsWithFilters(request, pageable)).thenReturn(page);

        ProductImage thumb = ProductImage.builder()
                .productImageId(9001L)
                .product(product1)
                .imageType(ImageType.THUMB)
                .imageUrl("https://cdn.example.com/thumb-101.jpg")
                .sortOrder(0)
                .build();

        FlavorNote flavorNote1 = FlavorNote.builder()
                .flavorNoteId(701L)
                .flavorCategory(FlavorCategory.NUTTY)
                .nameKo("초콜릿")
                .nameEn("Chocolate")
                .build();

        FlavorNote flavorNote2 = FlavorNote.builder()
                .flavorNoteId(702L)
                .flavorCategory(FlavorCategory.CHOCOLATY)
                .nameKo("캐러멜")
                .nameEn("Caramel")
                .build();

        ProductFlavorNote productFlavorNote1 = ProductFlavorNote.builder()
                .product(product1)
                .flavorNote(flavorNote1)
                .build();

        ProductFlavorNote productFlavorNote2 = ProductFlavorNote.builder()
                .product(product2)
                .flavorNote(flavorNote2)
                .build();

        when(productImageRepository.findByProduct_ProductIdInAndImageType(List.of(101L, 102L), ImageType.THUMB))
                .thenReturn(List.of(thumb));
        when(productFlavorNoteRepository.findByProduct_ProductIdIn(List.of(101L, 102L)))
                .thenReturn(List.of(productFlavorNote1, productFlavorNote2));

        PageResponse<ProductSummaryDTO> response = beanService.searchProducts(request, pageable);

        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.content()).hasSize(2);
        assertThat(response.content().getFirst().productId()).isEqualTo(101L);
        assertThat(response.content().getFirst().productImage()).isNotNull();
        assertThat(response.content().getFirst().productImage().imageType()).isEqualTo(ImageType.THUMB);
        assertThat(response.content().getFirst().flavorNotes()).isNotNull();
        assertThat(response.content().getFirst().flavorNotes().nameKo()).isEqualTo("초콜릿");
        assertThat(response.content().get(1).productId()).isEqualTo(102L);
        assertThat(response.content().get(1).productImage()).isNull();
        assertThat(response.content().get(1).flavorNotes()).isNotNull();
        assertThat(response.content().get(1).flavorNotes().nameKo()).isEqualTo("캐러멜");
    }

    @Test
    @DisplayName("원두 목록 조회: 최소값이 최대값보다 크면 600-2 예외를 던진다")
    void searchProducts_invalidRange_throwsCustomException() {
        ProductSearchRequest invalidRequest = new ProductSearchRequest(
                null, null,
                5.0, 3.0,
                null, null,
                null, null,
                null, null,
                null, null
        );
        Pageable pageable = PageRequest.of(0, 12);

        assertThatThrownBy(() -> beanService.searchProducts(invalidRequest, pageable))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BEAN_SEARCH_INVALID_RANGE);
    }

    @Test
    @DisplayName("원두 상세 조회: mock 엔티티로 썸네일/상세이미지/향미노트 매핑을 검증한다")
    void getProductDetail_withMockEntities_returnsDetail() {
        when(imageUrlResolver.toPublicUrl(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Long productId = 300L;

        Bean bean = Bean.builder()
                .nameKo("콜롬비아 수프리모")
                .nameEn("Colombia Supremo")
                .process("Washed")
                .origin("Colombia")
                .region("Huila")
                .build();

        Roaster roaster = Roaster.builder()
                .roasterId(77L)
                .nameKo("바리스테이션 로스터리")
                .nameEn("바리스테이션 Roastery")
                .homepageUrl("https://dripnote.example.com")
                .description("테스트용 로스터리")
                .build();

        Product product = Product.builder()
                .productId(productId)
                .roastLevel(RoastingType.MEDIUMDARK)
                .acidity(2.0)
                .sweetness(3.0)
                .body(4.0)
                .balance(3.0)
                .description("초콜릿과 견과류 중심의 밸런스 좋은 컵")
                .roaster(roaster)
                .build();

        BeanProduct selected = mock(BeanProduct.class);
        when(selected.getBean()).thenReturn(bean);
        when(selected.getProduct()).thenReturn(product);
        when(beanProductRepository.findByProductId(productId)).thenReturn(selected);

        ProductImage thumb = ProductImage.builder()
                .productImageId(1L)
                .product(product)
                .imageType(ImageType.THUMB)
                .imageUrl("https://cdn.example.com/thumb.jpg")
                .sortOrder(0)
                .build();

        ProductImage detail1 = ProductImage.builder()
                .productImageId(2L)
                .product(product)
                .imageType(ImageType.SUB)
                .imageUrl("https://cdn.example.com/detail-1.jpg")
                .sortOrder(1)
                .build();

        ProductImage detail2 = ProductImage.builder()
                .productImageId(3L)
                .product(product)
                .imageType(ImageType.SUB)
                .imageUrl("https://cdn.example.com/detail-2.jpg")
                .sortOrder(2)
                .build();

        when(productImageRepository.findByProduct_ProductIdOrderBySortOrderAsc(productId))
                .thenReturn(List.of(thumb, detail1, detail2));
        when(productImageRepository.findByProduct_ProductIdAndImageTypeOrderBySortOrderAsc(productId, ImageType.THUMB))
                .thenReturn(List.of(thumb));

        FlavorNote flavorNote = FlavorNote.builder()
                .flavorNoteId(501L)
                .flavorCategory(FlavorCategory.NUTTY)
                .nameKo("헤이즐넛")
                .nameEn("Hazelnut")
                .build();

        ProductFlavorNote productFlavorNote = ProductFlavorNote.builder()
                .product(product)
                .flavorNote(flavorNote)
                .build();

        when(productFlavorNoteRepository.findByProduct_ProductIdIn(List.of(productId)))
                .thenReturn(List.of(productFlavorNote));

        ProductDetailDTO detail = beanService.getProductDetail(productId, null);

        assertThat(detail.beanSummary().productId()).isEqualTo(productId);
        assertThat(detail.beanSummary().beanNameKo()).isEqualTo("콜롬비아 수프리모");
        assertThat(detail.beanSummary().productImage()).isNotNull();
        assertThat(detail.beanSummary().productImage().imageType()).isEqualTo(ImageType.THUMB);

        assertThat(detail.images()).hasSize(2);
        assertThat(detail.images()).extracting(ProductImageDTO::imageType).doesNotContain(ImageType.THUMB);

        assertThat(detail.flavorNotes()).hasSize(1);
        assertThat(detail.flavorNotes().getFirst().nameKo()).isEqualTo("헤이즐넛");
        assertThat(detail.roaster().nameKo()).isEqualTo("바리스테이션 로스터리");
        assertThat(detail.bookmarked()).isFalse();
    }

    @Test
    @DisplayName("원두 상세 조회: 로그인 사용자가 북마크한 상품이면 bookmarked=true를 반환한다")
    void getProductDetail_withBookmark_returnsBookmarkedTrue() {
        Long productId = 301L;
        Long userId = 7L;

        Bean bean = Bean.builder()
                .nameKo("브라질 산토스")
                .nameEn("Brazil Santos")
                .process("Natural")
                .origin("Brazil")
                .region("Minas Gerais")
                .build();

        Roaster roaster = Roaster.builder()
                .roasterId(78L)
                .nameKo("테스트 로스터리")
                .nameEn("Test Roastery")
                .homepageUrl("https://dripnote.example.com")
                .description("테스트용 로스터리")
                .build();

        Product product = Product.builder()
                .productId(productId)
                .roastLevel(RoastingType.MEDIUM)
                .acidity(3.0)
                .sweetness(3.0)
                .body(3.0)
                .balance(3.0)
                .description("견과류와 캐러멜의 밸런스")
                .roaster(roaster)
                .build();

        BeanProduct selected = mock(BeanProduct.class);
        when(selected.getBean()).thenReturn(bean);
        when(selected.getProduct()).thenReturn(product);
        when(beanProductRepository.findByProductId(productId)).thenReturn(selected);

        when(productImageRepository.findByProduct_ProductIdOrderBySortOrderAsc(productId))
                .thenReturn(List.of());
        when(productImageRepository.findByProduct_ProductIdAndImageTypeOrderBySortOrderAsc(productId, ImageType.THUMB))
                .thenReturn(List.of());
        when(productFlavorNoteRepository.findByProduct_ProductIdIn(List.of(productId)))
                .thenReturn(List.of());
        when(productBookmarkRepository.existsByUser_UserIdAndProduct_ProductId(userId, productId))
                .thenReturn(true);

        ProductDetailDTO detail = beanService.getProductDetail(productId, userId);

        assertThat(detail.bookmarked()).isTrue();
    }

    @Test
    @DisplayName("원두 상세 조회: 상품이 없으면 600-1 예외를 던진다")
    void getProductDetail_whenMissing_throwsCustomException() {
        when(beanProductRepository.findByProductId(any())).thenReturn(null);

        assertThatThrownBy(() -> beanService.getProductDetail(999L, null))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BEAN_NOT_FOUND);
    }

    @Test
    @DisplayName("원두 목록 조회: 조건이 아예 없는 경우")
    void searchProducts_withEmptyRequest_returnsPagedSummary() {
        when(imageUrlResolver.toPublicUrl(any())).thenAnswer(invocation -> invocation.getArgument(0));
        Pageable pageable = PageRequest.of(0, 12);
        ProductSearchRequest request = new ProductSearchRequest(
                null, null,
                null, null,
                null, null,
                null, null,
                null, null,
                null, null
        );

        Bean bean1 = Bean.builder()
                .nameKo("에티오피아 구지")
                .nameEn("Ethiopia Guji")
                .process("워시드")
                .origin("Ethiopia")
                .region("Guji")
                .build();

        Bean bean2 = Bean.builder()
                .nameKo("케냐 AA")
                .nameEn("Keynya AA")
                .process("내츄럴")
                .origin("Kenya")
                .region("Nyeri")
                .build();

        Product product1 = Product.builder()
                .productId(101L)
                .roastLevel(RoastingType.LIGHT)
                .acidity(4.0)
                .sweetness(3.0)
                .body(2.0)
                .balance(3.0)
                .build();

        Product product2 = Product.builder()
                .productId(102L)
                .roastLevel(RoastingType.MEDIUM)
                .acidity(3.0)
                .sweetness(4.0)
                .body(3.0)
                .balance(4.0)
                .build();

        // mock으로 대체 -> 원두와 상품 가져옴
        BeanProduct beanProduct1 = mock(BeanProduct.class);
        when(beanProduct1.getBean()).thenReturn(bean1);
        when(beanProduct1.getProduct()).thenReturn(product1);

        // 원두와 상품 2
        BeanProduct beanProduct2 = mock(BeanProduct.class);
        when(beanProduct2.getBean()).thenReturn(bean2);
        when(beanProduct2.getProduct()).thenReturn(product2);

        Page<BeanProduct> page = new PageImpl<>(List.of(beanProduct1, beanProduct2), pageable, 2);
        when(beanProductRepository.searchProductsWithFilters(request, pageable)).thenReturn(page);

        ProductImage thumb = ProductImage.builder()
                .productImageId(9001L)
                .product(product1)
                .imageType(ImageType.THUMB)
                .imageUrl("https://cdn.example.com/thumb-101.jpg")
                .sortOrder(0)
                .build();

        FlavorNote flavorNote1 = FlavorNote.builder()
                .flavorNoteId(703L)
                .flavorCategory(FlavorCategory.NUTTY)
                .nameKo("견과")
                .nameEn("Nutty")
                .build();

        ProductFlavorNote productFlavorNote1 = ProductFlavorNote.builder()
                .product(product1)
                .flavorNote(flavorNote1)
                .build();

        when(productImageRepository.findByProduct_ProductIdInAndImageType(List.of(101L, 102L), ImageType.THUMB))
                .thenReturn(List.of(thumb));
        when(productFlavorNoteRepository.findByProduct_ProductIdIn(List.of(101L, 102L)))
                .thenReturn(List.of(productFlavorNote1));

        PageResponse<ProductSummaryDTO> response = beanService.searchProducts(request, pageable);

        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.content()).hasSize(2);
        assertThat(response.content().getFirst().productId()).isEqualTo(101L);
        assertThat(response.content().getFirst().productImage()).isNotNull();
        assertThat(response.content().getFirst().productImage().imageType()).isEqualTo(ImageType.THUMB);
        assertThat(response.content().getFirst().flavorNotes()).isNotNull();
        assertThat(response.content().getFirst().flavorNotes().nameKo()).isEqualTo("견과");
        assertThat(response.content().get(1).productId()).isEqualTo(102L);
        assertThat(response.content().get(1).productImage()).isNull();
        assertThat(response.content().get(1).flavorNotes()).isNull();
    }
}


