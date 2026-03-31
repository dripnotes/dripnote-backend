package com.example.Dripnote.bean.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "beans")
public class BeanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bean_id")
    private Long beanId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roaster_id", nullable = false)
    private RoasterEntity roaster;

    @Column(name = "name_ko", nullable = false, length = 150)
    private String nameKo;

    @Column(name = "name_en", length = 150)
    private String nameEn;

    @Column(name = "roast_level", length = 30)
    private String roastLevel;

    @Column(name = "process", length = 100)
    private String process;

    @Column(name = "variety", length = 100)
    private String variety;

    @Column(name = "washing_station", length = 100)
    private String washingStation;

    @Column(name = "region", length = 100)
    private String region;

    @Column(name = "altitude_min")
    private Integer altitudeMin;

    @Column(name = "altitude_max")
    private Integer altitudeMax;

    @Column(name = "release_ym", length = 7)
    private String releaseYm;

    @Column(name = "sensory_narrative", columnDefinition = "TEXT")
    private String sensoryNarrative;

    @Column(name = "acidity_pct")
    private Integer acidityPct;

    @Column(name = "sweetness_pct")
    private Integer sweetnessPct;

    @Column(name = "body_pct")
    private Integer bodyPct;

    @Column(name = "roast_level_pct")
    private Integer roastLevelPct;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}