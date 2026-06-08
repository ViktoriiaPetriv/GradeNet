package org.bachelor.addservice.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "commission_member")
public class CommissionMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commission_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_cm_commission"))
    private Commission commission;

    @Column(name = "professor_id", nullable = false)
    private Long professorId;

    @Column(name = "is_head", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isHead = false;
}
