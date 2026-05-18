package org.bachelor.orgservice.repository;

import org.bachelor.orgservice.model.entity.SpecialtyOffering;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecialtyOfferingRepository extends JpaRepository<SpecialtyOffering, Long> {
    List<SpecialtyOffering> findAllBySpecialtyId(Long specialtyId);
    boolean existsBySpecialtyIdAndGraduationYear(Long specialtyId, Integer graduationYear);

    @Query("SELECT o.id FROM SpecialtyOffering o WHERE o.specialty.id IN :specialtyIds")
    List<Long> findIdsBySpecialtyIdIn(@Param("specialtyIds") List<Long> specialtyIds);

    java.util.Optional<SpecialtyOffering> findByExternalId(Long externalId);
}
