package dripnote.bean.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tasting_notes")
public class TastingNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tasting_note_id")
    private Long tastingNoteId;

    @Column(name = "name_ko", nullable = false, length = 50, unique = true)
    private String nameKo;

    @Column(name = "name_en", length = 50)
    private String nameEn;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
}