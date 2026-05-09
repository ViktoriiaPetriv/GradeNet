package org.bachelor.gradeservice.repository;

import org.bachelor.gradeservice.model.entity.HoursTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HoursTemplateRepository extends JpaRepository<HoursTemplate, Long> {
    Optional<HoursTemplate> findByEctsCreditsAndTotalHoursAndClassroomHoursAndLectureHoursAndSeminarHoursAndLaboratoryHoursAndIndividualHoursAndSelfWorkHours(
            Integer ectsCredits, Integer totalHours, Integer classroomHours,
            Integer lectureHours, Integer seminarHours, Integer laboratoryHours,
            Integer individualHours, Integer selfWorkHours
    );
}
