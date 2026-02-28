package org.bachelor.orgservice.repository;

import org.bachelor.orgservice.model.entity.Degree;
import org.bachelor.orgservice.model.entity.EduType;
import org.bachelor.orgservice.model.entity.Specialty;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class SpecialtySpecification {

    public static Specification<Specialty> hasOrganizationIdIn(List<Long> orgIds) {
        return (root, query, cb) ->
                orgIds == null ? null : root.get("organization").get("id").in(orgIds);
    }

    public static Specification<Specialty> hasDegree(Degree degree) {
        return (root, query, cb) ->
                degree == null ? null : cb.equal(root.get("degree"), degree);
    }

    public static Specification<Specialty> hasEduType(EduType eduType) {
        return (root, query, cb) ->
                eduType == null ? null : cb.equal(root.get("eduType"), eduType);
    }

    public static Specification<Specialty> hasCodeDegreeEduTypeOrg(
            String code, Degree degree, EduType eduType, Long orgId) {
        return (root, query, cb) -> {
            if (code == null || degree == null || eduType == null || orgId == null) {
                return null;
            }
            return cb.and(
                    cb.equal(root.get("code"), code),
                    cb.equal(root.get("degree"), degree),
                    cb.equal(root.get("eduType"), eduType),
                    cb.equal(root.get("organization").get("id"), orgId)
            );
        };
    }
}
