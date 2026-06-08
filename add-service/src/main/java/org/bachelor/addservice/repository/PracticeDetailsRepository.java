package org.bachelor.addservice.repository;

import org.bachelor.addservice.model.entity.PracticeDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PracticeDetailsRepository extends JpaRepository<PracticeDetails, Long> {
    Optional<PracticeDetails> findByAdditionalWorkId(Long additionalWorkId);
}
