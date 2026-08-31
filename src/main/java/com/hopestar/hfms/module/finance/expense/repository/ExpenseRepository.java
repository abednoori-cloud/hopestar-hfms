package com.hopestar.hfms.module.finance.expense.repository;

import com.hopestar.hfms.module.finance.expense.entity.Expense;
import com.hopestar.hfms.module.finance.expense.entity.ExpenseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * {@link JpaSpecificationExecutor} backs {@code ExpenseService}'s
 * multi-criteria search (category, status, date range, description),
 * mirroring {@code EmployeeAdvanceRepository} exactly.
 */
public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    Optional<Expense> findByIdAndActiveTrue(Long id);

    Optional<Expense> findByExpenseNumberAndActiveTrue(String expenseNumber);

    List<Expense> findByActiveTrueOrderByExpenseDateDesc();

    boolean existsByExpenseNumber(String expenseNumber);

    /**
     * Sums the USD-equivalent amount of every expense in the given status
     * within a date range -- for the Dashboard's "this month's expenses"
     * figure. Mirrors {@code TransactionRepository
     * .sumUsdEquivalentAmountByDirectionAndStatusAndDateRange} exactly:
     * same {@code POSTED}-only filtering, same inclusive date-range shape.
     */
    @Query("SELECT COALESCE(SUM(e.usdEquivalentAmount), 0) FROM Expense e "
            + "WHERE e.status = :status AND e.active = true AND e.expenseDate BETWEEN :start AND :end")
    BigDecimal sumUsdEquivalentAmountByStatusAndDateRange(@Param("status") ExpenseStatus status,
                                                            @Param("start") LocalDate start,
                                                            @Param("end") LocalDate end);
}
