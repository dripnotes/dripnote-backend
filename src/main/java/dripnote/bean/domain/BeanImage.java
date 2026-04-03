package dripnote.bean.domain;

import dripnote.bean.enums.ImageType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bean_images")
public class BeanImage {
    /**[수정된 부분]
     * @Enumerated(EnumType.STRING)
     * @Column(name = "image_type", length = 20)
     * private ImageType imageType;
     * ImageType을 ENUM으로 만들어서 Main 이미지와 Sub 이미지를 구분했습니다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bean_image_id")
    private Long beanImageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bean_id", nullable = false)
    private Bean bean;

    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", length = 20)
    private ImageType imageType;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}