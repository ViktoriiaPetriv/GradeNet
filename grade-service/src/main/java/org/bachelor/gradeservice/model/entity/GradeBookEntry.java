package org.bachelor.gradeservice.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "grade_book_entry",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_entry",
                columnNames = {"book_number_id", "specialty_discipline_id", "attempt"}
        )
)
public class GradeBookEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_number_id", nullable = false)
    private Long bookNumberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_discipline_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_entry_sd"))
    private SpecialtyDiscipline specialtyDiscipline;

    @Column(name = "professor_id", nullable = false)
    private Long professorId;

    @Column(name = "academic_year", nullable = false, length = 9)
    private String academicYear;

    @Column(nullable = false, columnDefinition = "INT DEFAULT 1")
    private Integer attempt = 1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EntryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EntryResult result;

    @Column(name = "report_date")
    private LocalDate reportDate;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Grade> grades = new ArrayList<>();
}
