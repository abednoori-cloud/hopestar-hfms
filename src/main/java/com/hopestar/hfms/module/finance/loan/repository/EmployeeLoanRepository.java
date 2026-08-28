package com.hopestar.hfms.module.finance.loan.repository;

import com.hopestar.hfms.module.finance.loan.entity.EmployeeLoan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * {@link JpaSpecificationExecutor} backs {@code EmployeeLoanService}'s
 * multi-criteria search (employee name/code, status), mirroring {@code
 * SalaryRepository}/{@code EmployeeRepository} in the other Finance
 * submodules.
 */
public interface EmployeeLoanRepository extends JpaRepository<EmployeeLoan, Long>, JpaSpecificationExecutor<EmployeeLoan> {

    Optional<EmployeeLoan> findByIdAndActiveTrue(Long id);

    Optional<EmployeeLoan> findByLoanNumberAndActiveTrue(String loanNumber);

    List<EmployeeLoan> findByEmployeeIdAndActiveTrueOrderByLoanDateDesc(Long employeeId);

    boolean existsByLoanNumber(String loanNumber);
}
