package com.hopestar.hfms.module.finance.expense.repository;

import com.hopestar.hfms.module.finance.expense.dto.ExpenseSearchDTO;
import com.hopestar.hfms.module.finance.expense.entity.Expense;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Builds a single {@link Specification} from whichever fields of an
 * {@link ExpenseSearchDTO} were actually supplied, mirroring {@code
 * EmployeeAdvanceSpecifications} exactly.
 */
@UtilityClass
public class ExpenseSpecifications {

    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("expenseNumber", "expenseDate", "usdEquivalentAmount");

    public Specification<Expense> fromSearchCriteria(ExpenseSearchDTO criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("active")));

            if (criteria.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), criteria.getCategoryId()));
            }
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            if (StringUtils.hasText(criteria.getDescription())) {
                predicates.add(cb.like(cb.lower(root.get("description")),
                        "%" + criteria.getDescription().toLowerCase() + "%"));
            }
            if (criteria.getDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("expenseDate"), criteria.getDateFrom()));
            }
            if (criteria.getDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("expenseDate"), criteria.getDateTo()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** Resolves a requested sort field to a safe, allow-listed entity property, defaulting to {@code expenseDate}. */
    public String resolveSortField(String requested) {
        return ALLOWED_SORT_FIELDS.contains(requested) ? requested : "expenseDate";
    }
}
