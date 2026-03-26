package org.bachelor.gradeservice.repository;

import org.bachelor.gradeservice.model.entity.Discipline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DisciplineRepository extends JpaRepository<Discipline, Long> {
    Optional<Discipline> findByName(String name);
}
