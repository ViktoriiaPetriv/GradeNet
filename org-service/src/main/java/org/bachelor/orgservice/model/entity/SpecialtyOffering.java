package org.bachelor.orgservice.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "specialty_offering",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_specialty_offering",
                columnNames = {"specialty_id", "graduation_year"}
        )
)
public class SpecialtyOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "specialty_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_so_specialty"))
    private Specialty specialty;

    @Column(name = "external_id")
    private Long externalId;

    @Column(name = "graduation_year", nullable = false)
    private Integer graduationYear;
}
