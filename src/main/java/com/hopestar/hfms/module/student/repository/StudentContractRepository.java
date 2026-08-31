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
     * Contracts in the given status with a positive remaining balance
     * (USD-equivalent contract value minus USD-equivalent posted payments),
     * optionally scoped to one program, computed, filtered, and sorted
     * entirely in one query -- each row is {@code Object[]{StudentContract
     * contract, BigDecimal remainingBalanceUsd}}. {@code student} and
     * {@code program} are both eagerly fetched so displaying the student's
     * name/code or the program's name per row never triggers a lazy-load
     * per row -- the Reports module's Student Report reads {@code
     * contract.getProgram().getName()} for every row, which would
     * otherwise be a fresh N+1 on top of the one this query already fixed
     * for {@code student}. Pass {@code Pageable.unpaged()} for
     * "every matching contract" (the Reports module's Student Report) or a
     * bounded {@code PageRequest} for "top N" (the Dashboard's outstanding-
     * receivables widget, {@code programId=null}) -- both reuse this same
     * query rather than duplicating it, replacing what was formerly a
     * "load a candidate pool, then run one remaining-balance query per
     * contract" approach (an N+1). Uses the same correlated-subquery shape
     * as {@link #countDistinctStudentsWithOutstandingBalance} -- just
     * repeated across the select/filter/order clauses since JPQL cannot
     * re-use a computed select-item alias in {@code WHERE}/{@code ORDER BY}.
     */
    @Query("SELECT c, c.usdEquivalentAmount - COALESCE((SELECT SUM(p1.usdEquivalentAmount) FROM StudentPayment p1 "
            + "WHERE p1.contract = c AND p1.status = :paymentStatus AND p1.active = true), 0) "
            + "FROM StudentContract c JOIN FETCH c.student JOIN FETCH c.program "
            + "WHERE c.active = true AND c.status = :status "
            + "AND (:programId IS NULL OR c.program.id = :programId) "
            + "AND c.usdEquivalentAmount > COALESCE((SELECT SUM(p2.usdEquivalentAmount) FROM StudentPayment p2 "
            + "WHERE p2.contract = c AND p2.status = :paymentStatus AND p2.active = true), 0) "
            + "ORDER BY (c.usdEquivalentAmount - COALESCE((SELECT SUM(p3.usdEquivalentAmount) FROM StudentPayment p3 "
            + "WHERE p3.contract = c AND p3.status = :paymentStatus AND p3.active = true), 0)) DESC")
    List<Object[]> findTopOutstandingContractsWithRemainingBalance(
            @Param("status") ContractStatus status, @Param("paymentStatus") PaymentStatus paymentStatus,
            @Param("programId") Long programId, Pageable pageable);

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
