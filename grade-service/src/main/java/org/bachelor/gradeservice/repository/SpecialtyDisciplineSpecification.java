package org.bachelor.gradeservice.repository;

import org.bachelor.gradeservice.model.entity.SpecialtyDiscipline;
import org.springframework.data.jpa.domain.Specification;

public class SpecialtyDisciplineSpecification {

    public static Specification<SpecialtyDiscipline> byDisciplineId(Long disciplineId) {
        return (root, query, cb) ->
                disciplineId == null ? null : cb.equal(root.get("discipline").get("id"), disciplineId);
    }

    public static Specification<SpecialtyDiscipline> bySpecialtyId(Long specialtyId) {
        return (root, query, cb) ->
                specialtyId == null ? null : cb.equal(root.get("specialtyId"), specialtyId);
    }
}