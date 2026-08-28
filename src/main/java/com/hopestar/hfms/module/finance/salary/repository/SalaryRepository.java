package com.hopestar.hfms.module.finance.salary.repository;

import com.hopestar.hfms.module.finance.salary.entity.Salary;
import com.hopestar.hfms.module.finance.salary.entity.SalaryPaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * {@link JpaSpecificationExecutor} backs {@code SalaryService}'s
 * multi-criteria search (employee, month/year, status), mirroring {@code
 * StudentRepository}/{@code EmployeeRepository} in the other modules.
 */
public interface SalaryRepository extends JpaRepository<Salary, Long>, JpaSpecificationExecutor<Salary> {

    Optional<Salary> findByIdAndActiveTrue(Long id);

    Optional<Salary> findBySalaryNumberAndActiveTrue(String salaryNumber);

    Optional<Salary> findByEmployeeIdAndMonthAndYearAndActiveTrue(Long employeeId, Integer month, Integer year);

    boolean existsByEmployeeIdAndMonthAndYear(Long employeeId, Integer month, Integer year);

    boolean existsByEmployeeIdAndMonthAndYearAndIdNot(Long employeeId, Integer month, Integer year, Long id);

    List<Salary> findByEmployeeIdAndActiveTrueOrderByYearDescMonthDesc(Long employeeId);

    boolean existsBySalaryNumber(String salaryNumber);

    /**
     * Count of salaries in the given status -- used by the Dashboard for
     * the "unpaid/pending salaries" count. A plain derived query, no
     * custom aggregation needed.
     */
    long countByPaymentStatusAndActiveTrue(SalaryPaymentStatus paymentStatus);

    /** Same as above, further scoped to one salary period. */
    long countByPaymentStatusAndMonthAndYearAndActiveTrue(SalaryPaymentStatus paymentStatus, Integer month, Integer year);

    /**
     * Sums the USD-equivalent of {@link Salary#getBasicSalary()} (not the
     * full net salary) across every salary in the given status. Only the
     * basic-salary component has a stored USD conversion ({@code
     * usdEquivalentSalary}, computed once at create/update time) -- bonus/
     * overtime/allowance/deductions that make up net salary are never
     * converted, so this is a deliberate, documented approximation, not a
     * full "total pending payroll in USD" figure. See the Dashboard
     * report's noted limitation.
     */
    @Query("SELECT COALESCE(SUM(s.usdEquivalentSalary), 0) FROM Salary s "
            + "WHERE s.paymentStatus = :status AND s.active = true")
    BigDecimal sumUsdEquivalentSalaryByPaymentStatusAndActiveTrue(@Param("status") SalaryPaymentStatus status);

    /**
     * Salaries in the given status, most recent period first, with {@code
     * employee} eagerly fetched -- used by the Dashboard's pending-salaries
     * widget so displaying the employee's name/code per row never triggers
     * a lazy-load per row.
     */
    @Query("SELECT s FROM Salary s JOIN FETCH s.employee "
            + "WHERE s.paymentStatus = :status AND s.active = true ORDER BY s.year DESC, s.month DESC")
    List<Salary> findByPaymentStatusAndActiveTrueWithEmployeeOrderByYearDescMonthDesc(
            @Param("status") SalaryPaymentStatus status, Pageable pageable);
}
