package baristation.bookmark.repository;

import baristation.bean.domain.ProductBookmark;
import baristation.bean.domain.Product;
import baristation.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductBookmarkRepository extends JpaRepository<ProductBookmark, Long> {
    Optional<ProductBookmark> findByUserAndProduct(User user, Product product);
    boolean existsByUser_UserIdAndProduct_ProductId(Long userId, Long productId);

    /**
     * 벌크 삭제: 특정 사용자의 모든 북마크 삭제
     * (회원 탈퇴 시 사용)
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ProductBookmark pb WHERE pb.user.userId = :userId")
    void deleteAllByUserIdInQuery(@Param("userId") Long userId);
}
