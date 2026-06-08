package org.bachelor.addservice.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "additional_work")
public class AdditionalWork {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "book_number_id", nullable = false)
    private Long bookNumberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_aw_commission"))
    private Commission commission;

    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(name = "university_grade")
    private Integer universityGrade;

    @Column(name = "national_grade", length = 20)
    private String nationalGrade;

    @Column(name = "ects_grade", length = 2)
    private String ectsGrade;

    @OneToOne(mappedBy = "additionalWork", cascade = CascadeType.ALL, orphanRemoval = true)
    private CourseWorkDetails courseWorkDetails;

    @OneToOne(mappedBy = "additionalWork", cascade = CascadeType.ALL, orphanRemoval = true)
    private PracticeDetails practiceDetails;

    @OneToOne(mappedBy = "additionalWork", cascade = CascadeType.ALL, orphanRemoval = true)
    private QualificationDetails qualificationDetails;
}
