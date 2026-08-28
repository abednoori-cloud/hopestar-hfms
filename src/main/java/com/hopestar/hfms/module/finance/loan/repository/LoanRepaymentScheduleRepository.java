package com.hopestar.hfms.module.finance.loan.repository;

import com.hopestar.hfms.module.finance.loan.entity.LoanRepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Plain {@link JpaRepository} -- repayments are only ever looked up by
 * their own id or by the parent loan, never multi-criteria searched, so
 * no {@code JpaSpecificationExecutor}/Specifications class is needed
 * here, unlike {@code EmployeeLoanRepository}.
 */
public interface LoanRepaymentScheduleRepository extends JpaRepository<LoanRepaymentSchedule, Long> {

    Optional<LoanRepaymentSchedule> findByIdAndActiveTrue(Long id);

    List<LoanRepaymentSchedule> findByLoanIdAndActiveTrueOrderByRepaymentDateDesc(Long loanId);
}
