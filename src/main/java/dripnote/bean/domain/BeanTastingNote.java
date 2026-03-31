package dripnote.bean.domain;

import dripnote.bean.enums.NoteType;
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
@Table(name = "bean_tasting_notes")
public class BeanTastingNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bean_tasting_note_id")
    private Long beanTastingNoteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bean_id", nullable = false)
    private Bean bean;

    @Column(name = "note_name", nullable = false, length = 50)
    private String noteName;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false, length = 10)
    private NoteType noteType = NoteType.SUB;

    @Column(name = "intensity")
    private Integer intensity;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}