package com.hopestar.hfms.module.finance.expense.repository;

import com.hopestar.hfms.module.finance.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}
