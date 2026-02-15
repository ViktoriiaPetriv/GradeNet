package org.bachelor.orgservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "specialty")
public class Specialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(name = "name_ua", nullable = false, unique = true)
    private String nameUA;

    @Column(name = "name_en", nullable = false, unique = true)
    private String nameEN;

    @Column(name = "study_program_ua", nullable = false)
    private String studyProgramUA;

    @Column(name = "study_program_en", nullable = false)
    private String studyProgramEN;

    @Column(name = "edu_program_ua", nullable = false)
    private String eduProgramUA;

    @Column(name = "edu_program_en", nullable = false)
    private String eduProgramEN;

    @ManyToOne
    @JoinColumn(name = "org_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "degree", nullable = false)
    private Degree degree;

    @Enumerated(EnumType.STRING)
    @Column(name = "edu_type", nullable = false)
    private EduType eduType;

    @Column(name = "start_date", nullable = false)
    private Instant startDate;

    @Column(name = "end_date")
    private Instant endDate;
}
