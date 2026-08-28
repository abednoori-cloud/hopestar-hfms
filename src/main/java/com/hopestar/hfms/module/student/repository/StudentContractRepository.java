package com.hopestar.hfms.module.student.repository;

import com.hopestar.hfms.module.finance.studentpayment.entity.PaymentStatus;
import com.hopestar.hfms.module.student.entity.ContractStatus;
import com.hopestar.hfms.module.student.entity.StudentContract;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StudentContractRepository extends JpaRepository<StudentContract, Long> {

    List<StudentContract> findByStudentIdAndActiveTrueOrderByContractDateDesc(Long studentId);

    Optional<StudentContract> findByIdAndActiveTrue(Long id);

    List<StudentContract> findByStudentIdAndStatusAndActiveTrue(Long studentId, ContractStatus status);

    long countByStudentIdAndActiveTrue(Long studentId);

    /**
     * Most recent contracts in the given status, with {@code student}
     * eagerly fetched in the same query -- used by the Dashboard's
     * outstanding-receivables widget so displaying the student's name/code
     * alongside each contract never triggers a lazy-load per row.
     */
    @Query("SELECT c FROM StudentContract c JOIN FETCH c.student "
            + "WHERE c.active = true AND c.status = :status ORDER BY c.contractDate DESC")
    List<StudentContract> findByStatusAndActiveTrueWithStudentOrderByContractDateDesc(
            @Param("status") ContractStatus status, Pageable pageable);

    /**
     * Total contract value (USD equivalent) of every contract in the given
     * status -- combined with {@code StudentPaymentRepository
     * .sumUsdEquivalentAmountByStatusAndContractStatus} in {@code
     * DashboardServiceImpl} to compute total outstanding receivables as
     * one subtraction of two independent sums, rather than a join that
     * would double-count a contract with more than one posted payment.
     */
    @Query("SELECT COALESCE(SUM(c.usdEquivalentAmount), 0) FROM StudentContract c "
            + "WHERE c.active = true AND c.status = :status")
    BigDecimal sumUsdEquivalentAmountByStatusAndActiveTrue(@Param("status") ContractStatus status);

    /**
     * Count of distinct students who have at least one contract in the
     * given status whose USD-equivalent value exceeds its posted-payment
     * total -- i.e. still owes money. The correlated subquery runs as a
     * single SQL statement (not one query per student), matching the
     * project's "no N+1" requirement for dashboard aggregates.
     */
    @Query("SELECT COUNT(DISTINCT c.student.id) FROM StudentContract c "
            + "WHERE c.active = true AND c.status = :status "
            + "AND c.usdEquivalentAmount > COALESCE((SELECT SUM(p.usdEquivalentAmount) FROM StudentPayment p "
            + "WHERE p.contract = c AND p.status = :paymentStatus AND p.active = true), 0)")
    long countDistinctStudentsWithOutstandingBalance(@Param("status") ContractStatus status,
                                                       @Param("paymentStatus") PaymentStatus paymentStatus);
}
