package org.bachelor.addservice.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "qualification_details")
public class QualificationDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "additional_work_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_qd_work"))
    private AdditionalWork additionalWork;

    @Column(name = "supervisor_id", nullable = false)
    private Long supervisorId;

    @Column(name = "state", nullable = false, length = 20, columnDefinition = "VARCHAR(20) DEFAULT 'IN_PROGRESS'")
    private String state = "IN_PROGRESS";
}
