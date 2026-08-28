package com.hopestar.hfms.module.finance.ledger.repository;

import com.hopestar.hfms.module.finance.ledger.dto.TransactionSearchDTO;
import com.hopestar.hfms.module.finance.ledger.entity.Transaction;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a single {@link Specification} from whichever fields of a
 * {@link TransactionSearchDTO} were actually supplied, mirroring {@code
 * StudentSpecifications} in the Student module.
 */
@UtilityClass
public class TransactionSpecifications {

    public Specification<Transaction> fromSearchCriteria(TransactionSearchDTO criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("active")));

            if (criteria.getTransactionType() != null) {
                predicates.add(cb.equal(root.get("transactionType"), criteria.getTransactionType()));
            }
            if (criteria.getDirection() != null) {
                predicates.add(cb.equal(root.get("direction"), criteria.getDirection()));
            }
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }
            if (criteria.getDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), criteria.getDateFrom()));
            }
            if (criteria.getDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), criteria.getDateTo()));
            }
            if (StringUtils.hasText(criteria.getReferenceTable())) {
                predicates.add(cb.equal(root.get("referenceTable"), criteria.getReferenceTable()));
            }
            if (criteria.getReferenceId() != null) {
                predicates.add(cb.equal(root.get("referenceId"), criteria.getReferenceId()));
            }
            if (criteria.getBranchId() != null) {
                predicates.add(cb.equal(root.get("branch").get("id"), criteria.getBranchId()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
