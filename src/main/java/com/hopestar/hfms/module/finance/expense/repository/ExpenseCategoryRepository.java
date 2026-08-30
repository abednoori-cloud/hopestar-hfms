package com.hopestar.hfms.module.finance.expense.repository;

import com.hopestar.hfms.module.finance.expense.entity.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory, Long> {

    List<ExpenseCategory> findByActiveTrueOrderByNameAsc();

    Optional<ExpenseCategory> findByNameAndActiveTrue(String name);

    boolean existsByName(String name);
}
