package org.bachelor.gradeservice.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "grade")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "attempt", nullable = false)
    private Integer attempt;

    @Column(name = "academic_year", nullable = false)
    private String academicYear;

    @Column(name = "semester", nullable = false)
    private Integer semester;

    @Column(name = "assessment_date")
    private Instant assessmentDate;

    @Column(name = "university_grade")
    private Integer universityGrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "national_grade")
    private NationalGrade nationalGrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "ects_grade")
    private EctsGrade ectsGrade;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment")
    private AssessmentType assessment;

    @Enumerated(EnumType.STRING)
    @Column(name = "state")
    private GradeState state;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_discipline_id", nullable = false)
    private SpecialtyDiscipline specialtyDiscipline;
}