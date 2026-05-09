package org.bachelor.gradeservice.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "hours_template")
public class HoursTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ects_credits", nullable = false)
    private Integer ectsCredits;

    @Column(name = "total_hours", nullable = false)
    private Integer totalHours;

    @Column(name = "classroom_hours", nullable = false)
    private Integer classroomHours;

    @Column(name = "lecture_hours", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer lectureHours = 0;

    @Column(name = "seminar_hours", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer seminarHours = 0;

    @Column(name = "laboratory_hours", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer laboratoryHours = 0;

    @Column(name = "individual_hours", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer individualHours = 0;

    @Column(name = "self_work_hours", nullable = false, columnDefinition = "INT DEFAULT 0")
    private Integer selfWorkHours = 0;
}
