package baristation.bean.service;

import baristation.bean.domain.Bean;
import baristation.bean.domain.BeanProduct;
import baristation.bean.domain.FlavorNote;
import baristation.bean.domain.Product;
import baristation.bean.domain.ProductFlavorNote;
import baristation.bean.domain.ProductImage;
import baristation.bean.domain.ProductBookmark;
import baristation.bean.domain.Roaster;
import baristation.bean.enums.BeanSortType;
import baristation.bean.enums.FlavorCategory;
import baristation.bean.enums.ImageType;
import baristation.bean.enums.RoastingType;
import baristation.bean.payload.dto.ProductDetailDTO;
import baristation.bean.payload.dto.ProductSummaryDTO;
import baristation.bean.payload.request.ProductSearchRequest;
import baristation.bean.repository.*;
import baristation.bookmark.repository.ProductBookmarkRepository;
import baristation.common.exception.CustomException;
import baristation.common.exception.ErrorCode;
import baristation.common.payload.response.PageResponse;
import baristation.common.r2.R2ImageService;
import baristation.user.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 통합 테스트: 실제 DB(H2)와 함께 동작하는 서비스 레이어 테스트
 *
 * Mock이 아닌 실제 데이터베이스와 QueryDSL 쿼리를 검증합니다.
 * 페이지네이션, 필터링, 정렬, 썸네일 매핑 등 전체 플로우를 검증합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Slf4j
class BeanServiceIntegrationTest {

    @Autowired
    private BeanService beanService;

    @Autowired
    private BeanRepository beanRepository;
    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BeanProductRepository beanProductRepository;

    @Autowired
    private RoastersRepository roastersRepository;

    @Autowired
    private ProductImageRepository productImageRepository;

    @Autowired
    private ProductFlavorNoteRepository productFlavorNoteRepository;

    @Autowired
    private ProductBookmarkRepository productBookmarkRepository;

    @Autowired
    private baristation.user.repository.UserRepository userRepository;

    @Autowired
    private FlavorNoteRepository flavorNoteRepository;

    @MockitoBean
    private R2ImageService r2ImageService;

    private Roaster roaster;
    private Bean ethiopiaBean;
    private Bean kenyaBean;
    private Bean colombiaBean;
    private Product ethiopiaProduct;
    private Product kenyaProduct;
    private Product colombiaProduct;

    @BeforeEach
    void setUp() {
        // 1. 로스터 생성
        roaster = Roaster.builder()
                .nameKo("드립노트 로스터리")
                .nameEn("Dripnote Roastery")
                .homepageUrl("https://dripnote.example.com")
                .description("프리미엄 스페셜티 커피 로스터")
                .build();
        roaster = roastersRepository.save(roaster);

        // 2. 원두 생성
        ethiopiaBean = Bean.builder()
                .nameKo("에티오피아 구지")
                .nameEn("Ethiopia Guji")
                .process("Washed")
                .origin("Ethiopia")
                .region("Guji")
                .build();

        kenyaBean = Bean.builder()
                .nameKo("케냐 AA")
                .nameEn("Kenya AA")
                .process("Natural")
                .origin("Kenya")
                .region("Nyeri")
                .build();

        colombiaBean = Bean.builder()
                .nameKo("콜롬비아 수프리모")
                .nameEn("Colombia Supremo")
                .process("Washed")
                .origin("Colombia")
                .region("Huila")
                .build();

        beanRepository.saveAll(List.of(ethiopiaBean, kenyaBean, colombiaBean));

        // 3. 상품 생성
        ethiopiaProduct = Product.builder()
                .nameKo("에티오피아 구지 라이트로스트")
                .nameEn("Ethiopia Guji Light Roast")
                .roastLevel(RoastingType.LIGHT)
                .acidity(5.0)
                .sweetness(4.0)
                .body(2.0)
                .balance(4.0)
                .agtronMin(70)
                .agtronMax(75)
                .roaster(roaster)
                .description("밝은 산미와 과일향이 특징")
                .build();

        kenyaProduct = Product.builder()
                .nameKo("케냐 AA 미디움로스트")
                .nameEn("Kenya AA Medium Roast")
                .roastLevel(RoastingType.MEDIUM)
                .acidity(4.0)
                .sweetness(4.0)
                .body(3.0)
                .balance(4.0)
                .agtronMin(55)
                .agtronMax(60)
                .roaster(roaster)
                .description("균형잡힌 산미와 단맛")
                .build();

        colombiaProduct = Product.builder()
                .nameKo("콜롬비아 수프리모 다크로스트")
                .nameEn("Colombia Supremo Dark Roast")
                .roastLevel(RoastingType.DARK)
                .acidity(2.0)
                .sweetness(3.0)
                .body(4.0)
                .balance(3.0)
                .agtronMin(35)
                .agtronMax(40)
                .roaster(roaster)
                .description("초콜릿과 견과류 중심의 진한 맛")
                .build();

        productRepository.saveAll(List.of(ethiopiaProduct, kenyaProduct, colombiaProduct));

        // 4. BeanProduct 생성 (원두와 상품 연결)
        BeanProduct ethiopiaBeanProduct = BeanProduct.builder()
                .bean(ethiopiaBean)
                .product(ethiopiaProduct)
                .build();

        BeanProduct kenyaBeanProduct = BeanProduct.builder()
                .bean(kenyaBean)
                .product(kenyaProduct)
                .build();

        BeanProduct colombiaBeanProduct = BeanProduct.builder()
                .bean(colombiaBean)
                .product(colombiaProduct)
                .build();

        beanProductRepository.saveAll(List.of(ethiopiaBeanProduct, kenyaBeanProduct, colombiaBeanProduct));

        // 5. 상품 이미지 생성 (썸네일 + 상세이미지)
        ProductImage ethiopiaThumb = ProductImage.builder()
                .product(ethiopiaProduct)
                .imageType(ImageType.THUMB)
                .imageUrl("https://cdn.example.com/ethiopia-thumb.jpg")
                .sortOrder(0)
                .build();

        ProductImage ethiopiaDetail = ProductImage.builder()
                .product(ethiopiaProduct)
                .imageType(ImageType.SUB)
                .imageUrl("https://cdn.example.com/ethiopia-detail.jpg")
                .sortOrder(1)
                .build();

        ProductImage kenyaThumb = ProductImage.builder()
                .product(kenyaProduct)
                .imageType(ImageType.THUMB)
                .imageUrl("https://cdn.example.com/kenya-thumb.jpg")
                .sortOrder(0)
                .build();

        ProductImage colombiaThumb = ProductImage.builder()
                .product(colombiaProduct)
                .imageType(ImageType.THUMB)
                .imageUrl("https://cdn.example.com/colombia-thumb.jpg")
                .sortOrder(0)
                .build();

        ProductImage colombiaDetail = ProductImage.builder()
                .product(colombiaProduct)
                .imageType(ImageType.SUB)
                .imageUrl("https://cdn.example.com/colombia-detail1.jpg")
                .sortOrder(1)
                .build();

        productImageRepository.saveAll(List.of(ethiopiaThumb, ethiopiaDetail, kenyaThumb, colombiaThumb, colombiaDetail));

        // 6. 향미 노트 생성
        FlavorNote fruitFlavor = FlavorNote.builder()
                .flavorCategory(FlavorCategory.FRUITY)
                .nameKo("베리")
                .nameEn("Berry")
                .build();

        FlavorNote floral = FlavorNote.builder()
                .flavorCategory(FlavorCategory.FLORAL)
                .nameKo("장미")
                .nameEn("Floral")
                .build();

        FlavorNote nutty = FlavorNote.builder()
                .flavorCategory(FlavorCategory.NUTTY)
                .nameKo("견과류")
                .nameEn("Nutty")
                .build();

        FlavorNote chocolate = FlavorNote.builder()
                .flavorCategory(FlavorCategory.CHOCOLATY)
                .nameKo("초콜릿")
                .nameEn("Chocolate")
                .build();

        flavorNoteRepository.saveAll(List.of(fruitFlavor, floral, nutty, chocolate));

        // 7. ProductFlavorNote 생성 (상품과 향미 연결)
        ProductFlavorNote ethiopiaFruit = ProductFlavorNote.builder()
                .product(ethiopiaProduct)
                .flavorNote(fruitFlavor)
                .build();

        ProductFlavorNote ethiopiaFloral = ProductFlavorNote.builder()
                .product(ethiopiaProduct)
                .flavorNote(floral)
                .build();

        ProductFlavorNote kenyaNutty = ProductFlavorNote.builder()
                .product(kenyaProduct)
                .flavorNote(nutty)
                .build();

        ProductFlavorNote colombiaChocolate = ProductFlavorNote.builder()
                .product(colombiaProduct)
                .flavorNote(chocolate)
                .build();

        ProductFlavorNote colombiaNutty = ProductFlavorNote.builder()
                .product(colombiaProduct)
                .flavorNote(nutty)
                .build();

        productFlavorNoteRepository.saveAll(List.of(ethiopiaFruit, ethiopiaFloral, kenyaNutty, colombiaChocolate, colombiaNutty));
    }

    @Test
    @DisplayName("통합 테스트: 기본 검색 조건으로 원두 목록 조회 - 페이지네이션 검증")
    void searchProducts_basicSearch_returnsPaginatedResults() {
        Pageable pageable = PageRequest.of(0, 2);
        ProductSearchRequest request = new ProductSearchRequest(
                null, null,
                0.1, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                null, BeanSortType.LATEST
        );

        PageResponse<ProductSummaryDTO> response = beanService.searchProducts(request, pageable);

        // 로그 출력
        log.info("[basicSearch] totalElements={} currentPage={} size={} hasNext={} contentSize={}",
                response.totalElements(), response.currentPage(), response.size(), response.hasNext(), response.content().size());
        response.content().forEach(dto -> log.info("[basicSearch] item: beanNameKo='{}' productId='{}' imageUrl='{}' flavorNote='{}'",
                dto.beanNameKo(),
                dto.productId(),
                dto.productImage() != null ? dto.productImage().imageUrl() : null,
                dto.flavorNotes() != null ? dto.flavorNotes().nameKo() : null));

        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.content()).hasSize(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.currentPage()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.content()).allSatisfy(dto -> assertThat(dto.flavorNotes()).isNotNull());
    }

    @Test
    @DisplayName("통합 테스트: 키워드 검색 - 'ethiopia' 포함하는 상품만 반환")
    void searchProducts_keywordFilter_returnsMatchingBeans() {
        Pageable pageable = PageRequest.of(0, 12);
        ProductSearchRequest request = new ProductSearchRequest(
                "ethiopia", null,
                0.1, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                null, null
        );

        PageResponse<ProductSummaryDTO> response = beanService.searchProducts(request, pageable);

        // 로그 출력
        log.info("[keywordFilter] totalElements={} contentSize={}", response.totalElements(), response.content().size());
        response.content().forEach(dto -> log.info("[keywordFilter] item: beanNameKo='{}' productId='{}' flavorNote='{}'",
                dto.beanNameKo(), dto.productId(), dto.flavorNotes() != null ? dto.flavorNotes().nameKo() : null));

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().beanNameKo()).contains("에티오피아");
        assertThat(response.content().getFirst().flavorNotes()).isNotNull();
    }

    @Test
    @DisplayName("통합 테스트: 로스팅 정도 필터 - LIGHT 로스팅만 반환")
    void searchProducts_roastingLevelFilter_returnsLightRoastOnly() {
        Pageable pageable = PageRequest.of(0, 12);
        ProductSearchRequest request = new ProductSearchRequest(
                null, null,
                0.1, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                RoastingType.LIGHT, null
        );

        PageResponse<ProductSummaryDTO> response = beanService.searchProducts(request, pageable);

        // 로그 출력
        log.info("[roastingFilter] totalElements={} contentSize={}", response.totalElements(), response.content().size());

        response.content().forEach(dto -> log.info("[roastingFilter] item: beanNameKo='{}' productId='{}' flavorNote='{}'",
                dto.beanNameKo(), dto.productId(), dto.flavorNotes() != null ? dto.flavorNotes().nameKo() : null));

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().getFirst().beanNameKo()).contains("에티오피아");
        assertThat(response.content().getFirst().flavorNotes()).isNotNull();
    }

    @Test
    @DisplayName("통합 테스트: 산미 범위 필터 - 산미 4~5만 반환")
    void searchProducts_acidityRangeFilter_returnsInRange() {
        Pageable pageable = PageRequest.of(0, 12);
        ProductSearchRequest request = new ProductSearchRequest(
                null, null,
                4.0, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                null, null
        );

        PageResponse<ProductSummaryDTO> response = beanService.searchProducts(request, pageable);

        // 로그 출력
        log.info("[acidityRange] totalElements={} contentSize={}", response.totalElements(), response.content().size());

        response.content().forEach(dto -> log.info("[acidityRange] item: beanNameKo='{}' productId='{}' flavorNote='{}'",
                dto.beanNameKo(), dto.productId(), dto.flavorNotes() != null ? dto.flavorNotes().nameKo() : null));

        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.content()).extracting(dto -> dto.beanNameKo())
                .contains("에티오피아 구지", "케냐 AA");
        assertThat(response.content()).allSatisfy(dto -> assertThat(dto.flavorNotes()).isNotNull());
    }

    @Test
    @DisplayName("통합 테스트: 향미 카테고리 필터 - NUTTY 카테고리만 반환")
    void searchProducts_flavorCategoryFilter_returnsNuttyFlavors() {
        Pageable pageable = PageRequest.of(0, 12);
        ProductSearchRequest request = new ProductSearchRequest(
                null, FlavorCategory.NUTTY,
                0.1, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                null, null
        );

        PageResponse<ProductSummaryDTO> response = beanService.searchProducts(request, pageable);

        // 로그 출력
        log.info("[flavorCategory] totalElements={} contentSize={}", response.totalElements(), response.content().size());

        response.content().forEach(dto -> log.info("[flavorCategory] item: beanNameKo='{}' productId='{}' productImageUrl='{}' flavorNote='{}'",
                dto.beanNameKo(), dto.productId(),
                dto.productImage() != null ? dto.productImage().imageUrl() : null,
                dto.flavorNotes() != null ? dto.flavorNotes().nameKo() : null));

        assertThat(response.totalElements()).isEqualTo(2);
        assertThat(response.content()).extracting(dto -> dto.beanNameKo())
                .contains("케냐 AA", "콜롬비아 수프리모");
        assertThat(response.content()).allSatisfy(dto -> assertThat(dto.flavorNotes()).isNotNull());
    }

    @Test
    @DisplayName("통합 테스트: 정렬 - 이름순(NAME)")
    void searchProducts_nameSort_returnsSortedByName() {
        Pageable pageable = PageRequest.of(0, 12);
        ProductSearchRequest request = new ProductSearchRequest(
                null, null,
                0.1, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                null, BeanSortType.NAME
        );

        PageResponse<ProductSummaryDTO> response = beanService.searchProducts(request, pageable);

        // 로그 출력
        log.info("[nameSort] contentSize={} order={} ", response.content().size(), BeanSortType.NAME);
        response.content().forEach(dto -> log.info("[nameSort] item: beanNameKo='{}' flavorNote='{}'", dto.beanNameKo(), dto.flavorNotes() != null ? dto.flavorNotes().nameKo() : null));

        assertThat(response.content()).extracting(dto -> dto.beanNameKo())
                .containsExactly("에티오피아 구지", "케냐 AA", "콜롬비아 수프리모");
        assertThat(response.content()).allSatisfy(dto -> assertThat(dto.flavorNotes()).isNotNull());
    }

    @Test
    @DisplayName("통합 테스트: 정렬 - 산미순(ACIDITY) 내림차순")
    void searchProducts_aciditySort_returnsSortedByAcidity() {
        Pageable pageable = PageRequest.of(0, 12);
        ProductSearchRequest request = new ProductSearchRequest(
                null, null,
                0.1, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                null, BeanSortType.ACIDITY
        );

        PageResponse<ProductSummaryDTO> response = beanService.searchProducts(request, pageable);

        // 로그 출력
        log.info("[aciditySort] contentSize={} order={} ", response.content().size(), BeanSortType.ACIDITY);

        response.content().forEach(dto -> log.info("[aciditySort] item: beanNameKo='{}' productId='{}' flavorNote='{}'", dto.beanNameKo(), dto.productId(), dto.flavorNotes() != null ? dto.flavorNotes().nameKo() : null));

        assertThat(response.content()).extracting(dto -> dto.beanNameKo())
                .containsExactly("에티오피아 구지", "케냐 AA", "콜롬비아 수프리모");
        assertThat(response.content()).allSatisfy(dto -> assertThat(dto.flavorNotes()).isNotNull());
    }

    @Test
    @DisplayName("통합 테스트: 썸네일 이미지 매핑 검증")
    void searchProducts_thumbnailMapping_mapsCorrectImages() {
        Pageable pageable = PageRequest.of(0, 12);
        ProductSearchRequest request = new ProductSearchRequest(
                null, null,
                0.1, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                null,null
        );

        PageResponse<ProductSummaryDTO> response = beanService.searchProducts(request, pageable);

        // 로그 출력
        log.info("[thumbnailMapping] contentSize={}", response.content().size());
        response.content().forEach(dto -> {
            if (dto.productImage() != null) {
                log.info("[thumbnailMapping] productId='{}' imageType='{}' imageUrl='{}' flavorNote='{}'",
                        dto.productId(), dto.productImage().imageType(), dto.productImage().imageUrl(),
                        dto.flavorNotes() != null ? dto.flavorNotes().nameKo() : null);
            } else {
                log.info("[thumbnailMapping] productId='{}' has no productImage flavorNote='{}'",
                        dto.productId(), dto.flavorNotes() != null ? dto.flavorNotes().nameKo() : null);
            }
        });

        assertThat(response.content())
                .allMatch(dto -> dto.productImage() != null, "모든 결과는 썸네일을 가져야 함")
                .allMatch(dto -> dto.productImage().imageType() == ImageType.THUMB, "모든 이미지는 THUMB이어야 함");
        assertThat(response.content()).allSatisfy(dto -> assertThat(dto.flavorNotes()).isNotNull());
    }

    @Test
    @DisplayName("통합 테스트: 범위 검증 오류 - 최소값 > 최대값")
    void searchProducts_invalidRange_throwsError() {
        ProductSearchRequest invalidRequest = new ProductSearchRequest(
                null, null,
                5.0, 3.0,
                null, null,
                null, null,
                null, null,
                null, null
        );

        log.info("[invalidRange] requesting min=5 max=3 -> expecting BeanSearchInvalidRange exception");

        assertThatThrownBy(() -> beanService.searchProducts(invalidRequest, PageRequest.of(0, 12)))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BEAN_SEARCH_INVALID_RANGE);
    }

    @Test
    @DisplayName("통합 테스트: 상세 조회 - 모든 필드 검증")
    void getProductDetail_returnsCompleteDetail() {
        ProductDetailDTO detail = beanService.getProductDetail(ethiopiaProduct.getProductId(), null);

        // 로그 출력
        log.info("[detail] productId='{}' beanName='{}' roaster='{}' acidity='{}' body='{}'",
                ethiopiaProduct.getProductId(),
                detail.beanSummary().beanNameKo(),
                detail.roaster() != null ? detail.roaster().nameKo() : null,
                detail.acidity(),
                detail.body());

        log.info("[detail] thumb='{}' imagesCount='{}' flavorNotes='{}'",
                detail.beanSummary().productImage() != null ? detail.beanSummary().productImage().imageUrl() : null,
                detail.images() != null ? detail.images().size() : 0,
                detail.flavorNotes() != null ? detail.flavorNotes().stream().map(fn -> fn.nameKo()).toList() : null);

        assertThat(detail).isNotNull();
        assertThat(detail.beanSummary().beanNameKo()).isEqualTo("에티오피아 구지");
        assertThat(detail.beanSummary().productImage()).isNotNull();
        assertThat(detail.beanSummary().productImage().imageType()).isEqualTo(ImageType.THUMB);

        assertThat(detail.images()).hasSize(1);
        assertThat(detail.images()).extracting(img -> img.imageType()).doesNotContain(ImageType.THUMB);

        assertThat(detail.flavorNotes()).hasSize(2);
        assertThat(detail.flavorNotes()).extracting(fn -> fn.nameKo())
                .contains("베리", "장미");

        assertThat(detail.roaster().nameKo()).isEqualTo("드립노트 로스터리");
        assertThat(detail.acidity()).isEqualTo(5.0);
        assertThat(detail.body()).isEqualTo(2.0);
        assertThat(detail.bookmarked()).isFalse();
    }

    @Test
    @DisplayName("통합 테스트: 상세 조회 - 로그인 사용자가 북마크한 경우 bookmarked=true")
    void getProductDetail_bookmarkedByUser_returnsTrue() {
        User user = userRepository.save(User.builder()
                .provider(baristation.user.enums.UserProvider.KAKAO)
                .providerId("bookmark-user-1")
                .nickname("bookmarkUser")
                .role(baristation.user.enums.UserRole.USER)
                .build());

        productBookmarkRepository.save(ProductBookmark.builder()
                .user(user)
                .product(ethiopiaProduct)
                .build());

        ProductDetailDTO detail = beanService.getProductDetail(ethiopiaProduct.getProductId(), user.getUserId());

        assertThat(detail.bookmarked()).isTrue();
    }

    @Test
    @DisplayName("통합 테스트: 상세 조회 - 로그인 사용자가 북마크하지 않은 경우 bookmarked=false")
    void getProductDetail_bookmarkedByUser_returnsFalse() {
        User user = userRepository.save(User.builder()
                .provider(baristation.user.enums.UserProvider.KAKAO)
                .providerId("bookmark-user-1")
                .nickname("bookmarkUser")
                .role(baristation.user.enums.UserRole.USER)
                .build());

        ProductDetailDTO detail = beanService.getProductDetail(ethiopiaProduct.getProductId(), user.getUserId());

        assertThat(detail.bookmarked()).isFalse();
    }

    @Test
    @DisplayName("통합 테스트: 상세 조회 - 존재하지 않는 상품")
    void getProductDetail_notFound_throwsError() {
        log.info("[detailNotFound] requesting productId=999999L -> expecting BEAN_NOT_FOUND");

        assertThatThrownBy(() -> beanService.getProductDetail(999999L, null))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BEAN_NOT_FOUND);
    }

    @Test
    @DisplayName("통합 테스트: 페이지 경계 - 마지막 페이지")
    void searchProducts_lastPage_returnsCorrectData() {
        Pageable pageable = PageRequest.of(1, 2);
        ProductSearchRequest request = new ProductSearchRequest(
                null, null,
                0.1, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                null, null
        );

        PageResponse<ProductSummaryDTO> response = beanService.searchProducts(request, pageable);

        // 로그 출력
        log.info("[lastPage] currentPage={} hasNext={} hasPrevious={} contentSize={}",
                response.currentPage(), response.hasNext(), response.hasPrevious(), response.content().size());
        response.content().forEach(dto -> log.info("[lastPage] item: beanNameKo='{}'", dto.beanNameKo()));

        assertThat(response.currentPage()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.hasPrevious()).isTrue();
        assertThat(response.content()).hasSize(1);
    }

    @Test
    @DisplayName("통합 테스트: 복합 필터 - 산미 범위 + 로스팅 정도")
    void searchProducts_combinedFilters_returnsFiltered() {
        Pageable pageable = PageRequest.of(0, 12);
        ProductSearchRequest request = new ProductSearchRequest(
                null, null,
                3.0, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                0.1, 5.0,
                RoastingType.MEDIUM, null
        );

        PageResponse<ProductSummaryDTO> response = beanService.searchProducts(request, pageable);

        // 로그 출력
        log.info("[combinedFilters] totalElements={} contentSize={}", response.totalElements(), response.content().size());

        response.content().forEach(dto -> log.info("[combinedFilters] item: beanNameKo='{}' productId='{}' flavorNote='{}'",
                dto.beanNameKo(), dto.productId(), dto.flavorNotes() != null ? dto.flavorNotes().nameKo() : null));

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.content()).extracting(dto -> dto.beanNameKo())
                .contains("케냐 AA");
        assertThat(response.content().getFirst().flavorNotes()).isNotNull();
    }
}
