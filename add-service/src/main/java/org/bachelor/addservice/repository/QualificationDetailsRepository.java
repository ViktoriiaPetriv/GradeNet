package org.bachelor.addservice.repository;

import org.bachelor.addservice.model.entity.QualificationDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QualificationDetailsRepository extends JpaRepository<QualificationDetails, Long> {
    Optional<QualificationDetails> findByAdditionalWorkId(Long additionalWorkId);
}
