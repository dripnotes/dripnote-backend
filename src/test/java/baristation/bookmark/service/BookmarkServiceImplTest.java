package baristation.bookmark.service;

import baristation.bean.domain.Bean;
import baristation.bean.domain.BeanProduct;
import baristation.bean.domain.Product;
import baristation.bean.domain.ProductBookmark;
import baristation.bean.domain.ProductImage;
import baristation.bean.enums.ImageType;
import baristation.bean.enums.RoastingType;
import baristation.bean.payload.dto.ProductSummaryDTO;
import baristation.bean.repository.BeanProductRepository;
import baristation.bean.repository.ProductFlavorNoteRepository;
import baristation.bean.repository.ProductImageRepository;
import baristation.bean.repository.ProductRepository;
import baristation.bookmark.repository.ProductBookmarkRepository;
import baristation.common.payload.response.PageResponse;
import baristation.common.r2.ImageUrlResolver;
import baristation.user.domain.User;
import baristation.user.enums.UserProvider;
import baristation.user.enums.UserRole;
import baristation.user.repository.UserRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceImplTest {

    @Mock private ProductBookmarkRepository productBookmarkRepository;
    @Mock private ProductRepository productRepository;
    @Mock private BeanProductRepository beanProductRepository;
    @Mock private UserRepository userRepository;
    @Mock private ImageUrlResolver imageUrlResolver;
    @Mock private ProductFlavorNoteRepository productFlavorNoteRepository;
    @Mock private ProductImageRepository productImageRepository;

    @InjectMocks
    private BookmarkServiceImpl bookmarkService;

    @Test
    @DisplayName("북마크 토글: 북마크가 없으면 생성한다")
    void toggleBookmark_whenMissing_savesBookmark() {
        User user = User.builder()
                .userId(1L)
                .provider(UserProvider.GOOGLE)
                .providerId("provider-1")
                .nickname("사용자")
                .role(UserRole.USER)
                .build();
        Product product = Product.builder().productId(10L).nameKo("원두").build();

        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productBookmarkRepository.findByUserAndProduct(user, product)).thenReturn(Optional.empty());

        bookmarkService.toggleBookmark(10L, 1L);

        verify(productBookmarkRepository).save(any(ProductBookmark.class));
    }

    @Test
    @DisplayName("북마크 토글: 북마크가 있으면 삭제한다")
    void toggleBookmark_whenExists_deletesBookmark() {
        User user = User.builder()
                .userId(1L)
                .provider(UserProvider.GOOGLE)
                .providerId("provider-1")
                .nickname("사용자")
                .role(UserRole.USER)
                .build();
        Product product = Product.builder().productId(10L).nameKo("원두").build();
        ProductBookmark bookmark = ProductBookmark.builder().bookmarkId(99L).user(user).product(product).build();

        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productBookmarkRepository.findByUserAndProduct(user, product)).thenReturn(Optional.of(bookmark));

        bookmarkService.toggleBookmark(10L, 1L);

        verify(productBookmarkRepository).delete(bookmark);
    }

    @Test
    @DisplayName("북마크 목록 조회: 북마크가 없으면 빈 페이지를 반환한다")
    void getBookmarks_whenEmpty_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 12);
        when(beanProductRepository.searchProductsWithUserId(pageable, 1L)).thenReturn(Page.empty(pageable));

        PageResponse<ProductSummaryDTO> response = bookmarkService.getBookmarks(1L, pageable);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("북마크 목록 조회: 향미가 없어도 null-safe 하게 DTO를 만든다")
    void getBookmarks_whenFlavorMissing_returnsNullSafeSummary() {
        Pageable pageable = PageRequest.of(0, 12);

        Bean bean = Bean.builder()
                .nameKo("에티오피아 구지")
                .nameEn("Ethiopia Guji")
                .process("Washed")
                .origin("Ethiopia")
                .region("Guji")
                .build();
        Product product = Product.builder()
                .productId(101L)
                .nameKo("구지 라이트")
                .roastLevel(RoastingType.LIGHT)
                .build();
        BeanProduct beanProduct = BeanProduct.builder()
                .bean(bean)
                .product(product)
                .build();
        Page<BeanProduct> page = new PageImpl<>(List.of(beanProduct), pageable, 1);

        ProductImage thumb = ProductImage.builder()
                .productImageId(1L)
                .product(product)
                .imageType(ImageType.THUMB)
                .imageUrl("/products/101/thumb.png")
                .sortOrder(0)
                .build();

        when(beanProductRepository.searchProductsWithUserId(pageable, 1L)).thenReturn(page);
        when(productImageRepository.findByProduct_ProductIdInAndImageType(List.of(101L), ImageType.THUMB))
                .thenReturn(List.of(thumb));
        when(productFlavorNoteRepository.findByProduct_ProductIdIn(List.of(101L)))
                .thenReturn(List.of());
        when(imageUrlResolver.toPublicUrl("/products/101/thumb.png")).thenReturn("https://cdn.example.com/products/101/thumb.png");

        PageResponse<ProductSummaryDTO> response = bookmarkService.getBookmarks(1L, pageable);

        assertThat(response.content()).hasSize(1);
        ProductSummaryDTO dto = response.content().getFirst();
        assertThat(dto.productId()).isEqualTo(101L);
        assertThat(dto.productImage()).isNotNull();
        assertThat(dto.productImage().imageUrl()).isEqualTo("https://cdn.example.com/products/101/thumb.png");
        assertThat(dto.flavorNotes()).isNull();
    }
}

