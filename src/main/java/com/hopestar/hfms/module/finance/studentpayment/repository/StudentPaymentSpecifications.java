package com.hopestar.hfms.module.finance.studentpayment.repository;

import com.hopestar.hfms.module.finance.studentpayment.dto.StudentPaymentSearchDTO;
import com.hopestar.hfms.module.finance.studentpayment.entity.StudentPayment;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a single {@link Specification} from whichever fields of a
 * {@link StudentPaymentSearchDTO} were actually supplied, mirroring
 * {@code StudentSpecifications} in the Student module.
 */
@UtilityClass
public class StudentPaymentSpecifications {

    public Specification<StudentPayment> fromSearchCriteria(StudentPaymentSearchDTO criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("active")));

            if (StringUtils.hasText(criteria.getPaymentNumber())) {
                predicates.add(cb.like(cb.lower(root.get("paymentNumber")),
                        "%" + criteria.getPaymentNumber().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(criteria.getReceiptNumber())) {
                predicates.add(cb.like(cb.lower(root.get("receiptNumber")),
                        "%" + criteria.getReceiptNumber().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(criteria.getStudentName())) {
                predicates.add(cb.like(cb.lower(root.get("student").get("fullName")),
                        "%" + criteria.getStudentName().toLowerCase() + "%"));
            }
            if (StringUtils.hasText(criteria.getStudentCode())) {
                predicates.add(cb.like(cb.lower(root.get("student").get("studentCode")),
                        "%" + criteria.getStudentCode().toLowerCase() + "%"));
            }
            if (criteria.getCurrencyCode() != null) {
                predicates.add(cb.equal(root.get("currency").get("code"), criteria.getCurrencyCode().name()));
            }
            if (criteria.getPaymentMethodId() != null) {
                predicates.add(cb.equal(root.get("paymentMethod").get("id"), criteria.getPaymentMethodId()));
            }
            if (criteria.getDateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("paymentDate"), criteria.getDateFrom()));
            }
            if (criteria.getDateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("paymentDate"), criteria.getDateTo()));
            }
            if (criteria.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), criteria.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
