package com.hopestar.hfms.module.finance.ledger.repository;

import com.hopestar.hfms.module.finance.ledger.entity.Direction;
import com.hopestar.hfms.module.finance.ledger.entity.Transaction;
import com.hopestar.hfms.module.finance.ledger.entity.TransactionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * {@link JpaSpecificationExecutor} backs {@code TransactionService}'s
 * multi-criteria search/filter (type, direction, status, date range,
 * branch, reference) for the Report and Dashboard modules (later phases),
 * the same pattern used for {@code StudentRepository} in the Student
 * module.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    Optional<Transaction> findByIdAndActiveTrue(Long id);

    Optional<Transaction> findByTransactionCodeAndActiveTrue(String transactionCode);

    List<Transaction> findByReferenceTableAndReferenceIdAndActiveTrueOrderByTransactionDateDesc(
            String referenceTable, Long referenceId);

    List<Transaction> findByReferenceTableAndReferenceIdAndStatusAndActiveTrue(
            String referenceTable, Long referenceId, TransactionStatus status);

    boolean existsByTransactionCode(String transactionCode);

    /**
     * Sums the USD-equivalent amount of every {@code POSTED} transaction in
     * one direction (income or expense) -- per the approved architecture,
     * this is what the Dashboard (Module 2) reads for its financial
     * summary, never re-deriving totals by summing the detail tables
     * (student_payments, salaries, ...) independently. {@code DRAFT} and
     * {@code VOIDED} transactions are excluded, matching the same "only
     * POSTED affects totals" rule used everywhere else in the Finance
     * module (e.g. {@code StudentPaymentRepository.sumUsdEquivalentAmountByContractIdAndStatus}).
     */
    @Query("SELECT COALESCE(SUM(t.usdEquivalentAmount), 0) FROM Transaction t "
            + "WHERE t.direction = :direction AND t.status = :status AND t.active = true")
    BigDecimal sumUsdEquivalentAmountByDirectionAndStatus(@Param("direction") Direction direction,
                                                            @Param("status") TransactionStatus status);

    /**
     * Same as {@link #sumUsdEquivalentAmountByDirectionAndStatus} but
     * scoped to a date range, for the Dashboard's current-period income/
     * expense figures.
     */
    @Query("SELECT COALESCE(SUM(t.usdEquivalentAmount), 0) FROM Transaction t "
            + "WHERE t.direction = :direction AND t.status = :status AND t.active = true "
            + "AND t.transactionDate BETWEEN :start AND :end")
    BigDecimal sumUsdEquivalentAmountByDirectionAndStatusAndDateRange(@Param("direction") Direction direction,
                                                                       @Param("status") TransactionStatus status,
                                                                       @Param("start") LocalDate start,
                                                                       @Param("end") LocalDate end);

    /**
     * Most recent transactions in the given status for the Dashboard's
     * "recent activity" widget, bounded by {@code pageable} rather than
     * loading the whole ledger, with {@code currency} eagerly fetched so
     * displaying the original currency code per row never triggers a
     * lazy-load per row.
     */
    @Query("SELECT t FROM Transaction t JOIN FETCH t.currency "
            + "WHERE t.status = :status AND t.active = true "
            + "ORDER BY t.transactionDate DESC, t.createdAt DESC")
    List<Transaction> findByStatusAndActiveTrueWithCurrencyOrderByDateDesc(
            @Param("status") TransactionStatus status, Pageable pageable);
}
