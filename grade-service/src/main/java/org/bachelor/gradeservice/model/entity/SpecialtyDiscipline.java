package org.bachelor.gradeservice.model.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;

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
//
//    @OneToOne(mappedBy = "specialtyDiscipline", cascade = CascadeType.ALL)
//    private Hours hours;
//
//    @OneToMany(mappedBy = "specialtyDiscipline")
//    private List<Grade> grades = new ArrayList<>();
}