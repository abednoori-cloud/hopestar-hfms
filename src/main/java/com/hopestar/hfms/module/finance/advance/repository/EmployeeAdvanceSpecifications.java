package com.hopestar.hfms.module.finance.advance.repository;

import com.hopestar.hfms.module.finance.advance.dto.EmployeeAdvanceSearchDTO;
import com.hopestar.hfms.module.finance.advance.entity.EmployeeAdvance;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builds a single {@link Specification} from whichever fields of an
 * {@link EmployeeAdvanceSearchDTO} were actually supplied, mirroring
 * {@code EmployeeLoanSpecifications} exactly.
 */
@UtilityClass
public class EmployeeAdvanceSpecifications {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("advanceNumber", "advanceDate", "remainingBalance");

    public Specification<EmployeeAdvance> fromSearchCriteria(EmployeeAdvanceSearchDTO criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("active")));

            if (StringUtils.hasText(criteria.getEmployeeName())) {
                predicates.add(cb.like(cb.lower(root.get("employee").get("fullName")),
                        "%" + criteria.getEmployeeName().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(criteria.getEmployeeCode())) {
                predicates.add(cb.like(cb.lower(root.get("employee").get("employeeCode")),
                        "%" + criteria.getEmployeeCode().toLowerCase() + "%"));
            }
            if (criteria.getEmployeeId() != null) {
                predicates.add(cb.equal(root.get("employee").get("id"), criteria.getEmployeeId()));
            }
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Resolves a requested sort field to a safe, allow-listed entity property, defaulting to {@code advanceDate}. */
    public String resolveSortField(String requested) {
        return ALLOWED_SORT_FIELDS.contains(requested) ? requested : "advanceDate";
    }
}
