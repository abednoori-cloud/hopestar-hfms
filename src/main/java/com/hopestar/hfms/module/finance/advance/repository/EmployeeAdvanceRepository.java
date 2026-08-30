package com.hopestar.hfms.module.finance.advance.repository;

import com.hopestar.hfms.module.finance.advance.entity.EmployeeAdvance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

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
}
