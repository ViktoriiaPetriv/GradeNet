package org.bachelor.gradeservice.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hours")
public class Hours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_discipline_id", nullable = false)
    private SpecialtyDiscipline specialtyDiscipline;

    @Column(name = "ects_hours")
    private Integer ectsHours;

    @Column(name = "all_hours")
    private Integer allHours;

    @Column(name = "total_classroom_hours")
    private Integer totalClassroomHours;

    private Integer lecture;
    private Integer seminar;
    private Integer laboratory;
    private Integer individual;

    @Column(name = "self_work")
    private Integer selfWork;
}