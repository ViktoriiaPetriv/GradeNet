package org.bachelor.gradeservice.repository;

import org.bachelor.gradeservice.model.entity.SpecialtyDiscipline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialtyDisciplineRepository extends JpaRepository<SpecialtyDiscipline, Long>, JpaSpecificationExecutor<SpecialtyDiscipline> {
    boolean existsBySpecialtyOfferingId(Long specialtyOfferingId);
    boolean existsBySpecialtyOfferingIdAndDisciplineId(Long specialtyOfferingId, Long disciplineId);
    Optional<SpecialtyDiscipline> findBySpecialtyOfferingIdAndDisciplineId(Long specialtyOfferingId, Long disciplineId);
}
