package org.bachelor.gradeservice.repository;

import org.bachelor.gradeservice.model.entity.SpecialtyDiscipline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecialtyDisciplineRepository extends JpaRepository<SpecialtyDiscipline, Long> {
    List<SpecialtyDiscipline> findAllBySpecialtyId(Long specialtyId);
    boolean existsByDisciplineIdAndSpecialtyId(Long disciplineId, Long specialtyId);
}