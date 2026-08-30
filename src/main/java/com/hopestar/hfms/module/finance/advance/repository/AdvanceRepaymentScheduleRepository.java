package com.hopestar.hfms.module.finance.advance.repository;

import com.hopestar.hfms.module.finance.advance.entity.AdvanceRepaymentSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Plain {@link JpaRepository} -- repayments are only ever looked up by
 * their own id or by the parent advance, never multi-criteria searched,
 * so no {@code JpaSpecificationExecutor}/Specifications class is needed
 * here, unlike {@code EmployeeAdvanceRepository}. Mirrors {@code
 * LoanRepaymentScheduleRepository} exactly.
 */
public interface AdvanceRepaymentScheduleRepository extends JpaRepository<AdvanceRepaymentSchedule, Long> {

    Optional<AdvanceRepaymentSchedule> findByIdAndActiveTrue(Long id);

    List<AdvanceRepaymentSchedule> findByAdvanceIdAndActiveTrueOrderByRepaymentDateDesc(Long advanceId);
}
