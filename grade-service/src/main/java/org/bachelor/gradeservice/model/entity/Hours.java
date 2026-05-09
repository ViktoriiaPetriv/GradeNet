package org.bachelor.gradeservice.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@Entity
@Table(name = "hours")
public class Hours {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_discipline_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_hours_sd"))
    private SpecialtyDiscipline specialtyDiscipline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private HoursTemplate template;

    @Column(name = "academic_year", nullable = false, length = 9)
    private String academicYear;
}
