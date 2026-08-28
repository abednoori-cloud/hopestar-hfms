package com.hopestar.hfms.module.finance.loan.repository;

import com.hopestar.hfms.module.finance.loan.dto.EmployeeLoanSearchDTO;
import com.hopestar.hfms.module.finance.loan.entity.EmployeeLoan;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builds a single {@link Specification} from whichever fields of an
 * {@link EmployeeLoanSearchDTO} were actually supplied, mirroring {@code
 * SalarySpecifications} exactly.
 */
@UtilityClass
public class EmployeeLoanSpecifications {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("loanNumber", "loanDate", "remainingBalance");

    public Specification<EmployeeLoan> fromSearchCriteria(EmployeeLoanSearchDTO criteria) {
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

    /** Resolves a requested sort field to a safe, allow-listed entity property, defaulting to {@code loanDate}. */
    public String resolveSortField(String requested) {
        return ALLOWED_SORT_FIELDS.contains(requested) ? requested : "loanDate";
    }
}
