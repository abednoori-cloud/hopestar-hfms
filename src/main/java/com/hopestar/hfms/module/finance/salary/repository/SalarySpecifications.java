package com.hopestar.hfms.module.finance.salary.repository;

import com.hopestar.hfms.module.finance.salary.dto.SalarySearchDTO;
import com.hopestar.hfms.module.finance.salary.entity.Salary;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builds a single {@link Specification} from whichever fields of a
 * {@link SalarySearchDTO} were actually supplied, mirroring {@code
 * StudentSpecifications}/{@code EmployeeSpecifications} exactly.
 */
@UtilityClass
public class SalarySpecifications {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("salaryNumber", "year", "month", "netSalary");

    public Specification<Salary> fromSearchCriteria(SalarySearchDTO criteria) {
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
            if (criteria.getMonth() != null) {
                predicates.add(cb.equal(root.get("month"), criteria.getMonth()));
            }
            if (criteria.getYear() != null) {
                predicates.add(cb.equal(root.get("year"), criteria.getYear()));
            }
            if (criteria.getPaymentStatus() != null) {
                predicates.add(cb.equal(root.get("paymentStatus"), criteria.getPaymentStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Resolves a requested sort field to a safe, allow-listed entity property, defaulting to {@code year}. */
    public String resolveSortField(String requested) {
        return ALLOWED_SORT_FIELDS.contains(requested) ? requested : "year";
    }
}
