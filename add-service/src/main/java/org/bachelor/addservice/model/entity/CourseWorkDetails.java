package org.bachelor.addservice.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "course_work_details")
public class CourseWorkDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "additional_work_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_cwd_work"))
    private AdditionalWork additionalWork;

    @Column(name = "semester", nullable = false)
    private Integer semester;

    @Column(name = "state", nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'IN_PROGRESS'")
    private String state = "IN_PROGRESS";

    @Column(name = "ects_credits")
    private Integer ectsCredits;

    @Column(name = "total_hours")
    private Integer totalHours;
}
