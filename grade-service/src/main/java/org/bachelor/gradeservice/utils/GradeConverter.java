package org.bachelor.gradeservice.utils;

import org.bachelor.gradeservice.model.entity.AssessmentType;
import org.bachelor.gradeservice.model.entity.EctsGrade;
import org.bachelor.gradeservice.model.entity.NationalGrade;
import org.springframework.stereotype.Component;

@Component
public class GradeConverter {

    public NationalGrade toNational(Integer universityGrade, AssessmentType assessmentType) {
        if (universityGrade == null || assessmentType == null) return null;

        return switch (assessmentType) {
            case EXAM -> toNationalExam(universityGrade);
            case CREDIT -> toNationalCredit(universityGrade);
        };
    }

    public EctsGrade toEcts(Integer universityGrade) {
        if (universityGrade == null) return null;
        if (universityGrade >= 90) return EctsGrade.A;
        if (universityGrade >= 80) return EctsGrade.B;
        if (universityGrade >= 70) return EctsGrade.C;
        if (universityGrade >= 60) return EctsGrade.D;
        if (universityGrade >= 50) return EctsGrade.E;
        if (universityGrade >= 26) return EctsGrade.FE;
        return EctsGrade.F;
    }

    private NationalGrade toNationalExam(Integer universityGrade) {
        if (universityGrade >= 90) return NationalGrade.FIVE;
        if (universityGrade >= 70) return NationalGrade.FOUR;
        if (universityGrade >= 50) return NationalGrade.THREE;
        return NationalGrade.TWO;
    }

    private NationalGrade toNationalCredit(Integer universityGrade) {
        return universityGrade >= 60 ? NationalGrade.PASSED : NationalGrade.NOT_PASSED;
    }
}
