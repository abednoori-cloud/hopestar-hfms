package com.hopestar.hfms.module.finance.employee.repository;

import com.hopestar.hfms.module.finance.employee.dto.EmployeeSearchDTO;
import com.hopestar.hfms.module.finance.employee.entity.Employee;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builds a single {@link Specification} from whichever fields of an
 * {@link EmployeeSearchDTO} were actually supplied, mirroring {@code
 * StudentSpecifications} in the Student module exactly.
 */
@UtilityClass
public class EmployeeSpecifications {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("fullName", "employeeCode", "joiningDate", "baseSalary");

    public Specification<Employee> fromSearchCriteria(EmployeeSearchDTO criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("active")));

            if (StringUtils.hasText(criteria.getName())) {
                predicates.add(cb.like(cb.lower(root.get("fullName")),
                        "%" + criteria.getName().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(criteria.getEmployeeCode())) {
                predicates.add(cb.like(cb.lower(root.get("employeeCode")),
                        "%" + criteria.getEmployeeCode().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(criteria.getPhone())) {
                predicates.add(cb.like(root.get("phone"), "%" + criteria.getPhone() + "%"));
            }
            if (StringUtils.hasText(criteria.getEmail())) {
                predicates.add(cb.like(cb.lower(root.get("email")),
                        "%" + criteria.getEmail().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(criteria.getDepartment())) {
                predicates.add(cb.like(cb.lower(root.get("department")),
                        "%" + criteria.getDepartment().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(criteria.getPosition())) {
                predicates.add(cb.like(cb.lower(root.get("position")),
                        "%" + criteria.getPosition().toLowerCase() + "%"));
            }
            if (criteria.getEmploymentStatusId() != null) {
                predicates.add(cb.equal(root.get("employmentStatus").get("id"), criteria.getEmploymentStatusId()));
            }
            if (criteria.getBranchId() != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), criteria.getBranchId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Resolves a requested sort field to a safe, allow-listed entity property, defaulting to {@code fullName}. */
    public String resolveSortField(String requested) {
        return ALLOWED_SORT_FIELDS.contains(requested) ? requested : "fullName";
    }
}
