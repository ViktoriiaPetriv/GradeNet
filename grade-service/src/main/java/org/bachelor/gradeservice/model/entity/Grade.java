package org.bachelor.gradeservice.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Entity representing an individual grade record.
 * Stores the actual grade values in different grading systems and the
 * assessment date.
 */
@Getter
@Setter
@Entity
@Table(name = "grade")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entry_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_grade_entry"))
    private GradeBookEntry entry;

    @Column(name = "assessment_date", nullable = false)
    private LocalDateTime assessmentDate;

    @Column(name = "university_grade")
    private Integer universityGrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "national_grade", length = 20)
    private NationalGrade nationalGrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "ects_grade", length = 2)
    private EctsGrade ectsGrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_type", length = 20)
    private AssessmentType assessmentType;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}
