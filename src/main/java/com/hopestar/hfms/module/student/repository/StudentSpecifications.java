package com.hopestar.hfms.module.student.repository;

import com.hopestar.hfms.module.student.dto.StudentSearchDTO;
import com.hopestar.hfms.module.student.entity.Student;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a single {@link Specification} from whichever fields of a
 * {@link StudentSearchDTO} were actually supplied, so the student list
 * page can filter on any combination of name/code/passport/phone/email/
 * program/country/status without a combinatorial explosion of repository
 * finder methods.
 */
@UtilityClass
public class StudentSpecifications {

    public Specification<Student> fromSearchCriteria(StudentSearchDTO criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only active (non-soft-deleted) students appear in search
            // results by default.
            predicates.add(cb.isTrue(root.get("active")));

            if (StringUtils.hasText(criteria.getName())) {
                predicates.add(cb.like(cb.lower(root.get("fullName")),
                        "%" + criteria.getName().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(criteria.getStudentCode())) {
                predicates.add(cb.like(cb.lower(root.get("studentCode")),
                        "%" + criteria.getStudentCode().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(criteria.getPassportNumber())) {
                predicates.add(cb.equal(cb.lower(root.get("passportNumber")),
                        criteria.getPassportNumber().toLowerCase()));
            }
            if (StringUtils.hasText(criteria.getPhone())) {
                predicates.add(cb.like(root.get("phone"), "%" + criteria.getPhone() + "%"));
            }
            if (StringUtils.hasText(criteria.getEmail())) {
                predicates.add(cb.like(cb.lower(root.get("email")),
                        "%" + criteria.getEmail().toLowerCase() + "%"));
            }
            if (criteria.getProgramId() != null) {
                predicates.add(cb.equal(root.get("program").get("id"), criteria.getProgramId()));
            }
            if (StringUtils.hasText(criteria.getDestinationCountry())) {
                predicates.add(cb.like(cb.lower(root.get("destinationCountry")),
                        "%" + criteria.getDestinationCountry().toLowerCase() + "%"));
            }
            if (criteria.getStatusId() != null) {
                predicates.add(cb.equal(root.get("status").get("id"), criteria.getStatusId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
