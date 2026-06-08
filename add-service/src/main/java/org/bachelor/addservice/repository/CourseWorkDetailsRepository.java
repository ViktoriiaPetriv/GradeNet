package org.bachelor.addservice.repository;

import org.bachelor.addservice.model.entity.CourseWorkDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CourseWorkDetailsRepository extends JpaRepository<CourseWorkDetails, Long> {
    Optional<CourseWorkDetails> findByAdditionalWorkId(Long additionalWorkId);
}
