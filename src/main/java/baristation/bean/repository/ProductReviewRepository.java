package baristation.bean.repository;

import baristation.bean.domain.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    /**
     * 상품 리뷰의 user_id를 1(탈퇴 유저)으로 덮어씌우기
     * 회원 탈퇴 시 리뷰 데이터는 보존하되 사용자 정보를 제거합니다.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE product_reviews SET user_id = 1 WHERE user_id = :userId", nativeQuery = true)
    void updateUserIdToDeletedByUserIdInQuery(@Param("userId") Long userId);
}
