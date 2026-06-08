package org.bachelor.addservice.repository;

import org.bachelor.addservice.model.entity.AdditionalWork;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdditionalWorkRepository extends JpaRepository<AdditionalWork, Long> {
    List<AdditionalWork> findAllByCommissionId(Long commissionId);
    List<AdditionalWork> findAllByBookNumberId(Long bookNumberId);
}
