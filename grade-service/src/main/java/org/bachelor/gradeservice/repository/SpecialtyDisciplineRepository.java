package org.bachelor.gradeservice.repository;

import org.bachelor.gradeservice.model.entity.SpecialtyDiscipline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpecialtyDisciplineRepository extends JpaRepository<SpecialtyDiscipline, Long>, JpaSpecificationExecutor<SpecialtyDiscipline> {
    boolean existsBySpecialtyIdAndDisciplineId(Long specialtyId, Long disciplineId);
    Optional<SpecialtyDiscipline> findBySpecialtyIdAndDisciplineId(Long specialtyId, Long disciplineId);
}
