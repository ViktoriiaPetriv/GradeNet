package org.bachelor.orgservice.repository;

import org.bachelor.orgservice.model.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Long>, JpaSpecificationExecutor<Specialty> {
    Optional<Specialty> findByCode(String code);
    Optional<Specialty> findByNameUA(String nameUA);
    Optional<Specialty> findByNameEN(String nameEN);
}
