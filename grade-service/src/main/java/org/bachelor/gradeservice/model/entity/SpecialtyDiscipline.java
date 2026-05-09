package org.bachelor.gradeservice.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Getter
@Setter
@Entity
@Table(name = "specialty_discipline",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_specialty_discipline",
                columnNames = {"specialty_id", "discipline_id"}
        )
)
public class SpecialtyDiscipline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "specialty_id", nullable = false)
    private Long specialtyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discipline_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_sd_discipline"))
    private Discipline discipline;

    @OneToMany(mappedBy = "specialtyDiscipline", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Hours> hours = new HashSet<>();

    @OneToMany(mappedBy = "specialtyDiscipline", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GradeBookEntry> gradeBookEntries = new ArrayList<>();
}
