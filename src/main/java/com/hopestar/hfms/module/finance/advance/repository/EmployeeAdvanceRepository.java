package com.hopestar.hfms.module.finance.advance.repository;

import com.hopestar.hfms.module.finance.advance.entity.AdvanceStatus;
import com.hopestar.hfms.module.finance.advance.entity.EmployeeAdvance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * {@link JpaSpecificationExecutor} backs {@code EmployeeAdvanceService}'s
 * multi-criteria search (employee name/code, status), mirroring {@code
 * EmployeeLoanRepository} exactly.
 */
public interface EmployeeAdvanceRepository extends JpaRepository<EmployeeAdvance, Long>, JpaSpecificationExecutor<EmployeeAdvance> {

    Optional<EmployeeAdvance> findByIdAndActiveTrue(Long id);

    Optional<EmployeeAdvance> findByAdvanceNumberAndActiveTrue(String advanceNumber);

    List<EmployeeAdvance> findByEmployeeIdAndActiveTrueOrderByAdvanceDateDesc(Long employeeId);

    boolean existsByAdvanceNumber(String advanceNumber);

    /** Count of advances in the given status, for the Dashboard's "active advances" figure. */
    long countByStatusAndActiveTrue(AdvanceStatus status);

    /**
     * Sums the USD-equivalent remaining balance of every advance in the
     * given status, for the Dashboard's "advances outstanding" figure.
     * Mirrors {@code EmployeeLoanRepository
     * .sumRemainingBalanceUsdByStatusAndActiveTrue} exactly -- {@code
     * remainingBalance} is stored in the advance's own currency, so it is
     * multiplied by the stored {@code exchangeRateToUsd} rather than
     * re-deriving a rate.
     */
    @Query("SELECT COALESCE(SUM(a.remainingBalance * a.exchangeRateToUsd), 0) FROM EmployeeAdvance a "
            + "WHERE a.status = :status AND a.active = true")
    BigDecimal sumRemainingBalanceUsdByStatusAndActiveTrue(@Param("status") AdvanceStatus status);

    /**
     * Per-employee USD-equivalent remaining balance of every advance in
     * the given status, one row per employee -- backs the Reports module's
     * Employee Report. Mirrors {@code EmployeeLoanRepository
     * .sumRemainingBalanceUsdGroupedByEmployeeAndStatus} exactly.
     */
    @Query("SELECT a.employee.id, a.employee.employeeCode, a.employee.fullName, "
            + "COALESCE(SUM(a.remainingBalance * a.exchangeRateToUsd), 0) FROM EmployeeAdvance a "
            + "WHERE a.status = :status AND a.active = true "
            + "GROUP BY a.employee.id, a.employee.employeeCode, a.employee.fullName")
    List<Object[]> sumRemainingBalanceUsdGroupedByEmployeeAndStatus(@Param("status") AdvanceStatus status);
}
