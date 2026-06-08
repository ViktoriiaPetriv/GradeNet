package org.bachelor.addservice.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "practice_details")
public class PracticeDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "additional_work_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_pd_work"))
    private AdditionalWork additionalWork;

    @Column(name = "organization", nullable = false, length = 255)
    private String organization;

    @Column(name = "course", nullable = false)
    private Integer course;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "work_description", columnDefinition = "TEXT")
    private String workDescription;

    @Column(name = "ects_credits", nullable = false)
    private Integer ectsCredits;

    @Column(name = "total_hours")
    private Integer totalHours;

    @Column(name = "supervisor_id", nullable = false)
    private Long supervisorId;
}
