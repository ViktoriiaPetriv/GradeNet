package org.bachelor.gradeservice.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "specialty_discipline")
public class SpecialtyDiscipline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "specialty_id", nullable = false)
    private Long specialtyId;

    @Column(name = "professor_id")
    private Long professorId;

    @Column(name = "report_date")
    private Instant reportDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discipline_id", nullable = false)
    private Discipline discipline;

    @OneToOne(mappedBy = "specialtyDiscipline", cascade = CascadeType.ALL, orphanRemoval = true)
    private Hours hours;

    @OneToMany(mappedBy = "specialtyDiscipline", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Grade> grades = new ArrayList<>();
}