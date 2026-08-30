package com.hopestar.hfms.module.finance.employee.repository;

import com.hopestar.hfms.module.finance.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

/**
 * {@link JpaSpecificationExecutor} backs {@code EmployeeService}'s
 * multi-criteria search (name/code/phone/email/department/position/
 * status/branch), mirroring {@code StudentRepository} in the Student
 * module.
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByIdAndActiveTrue(Long id);

    List<Employee> findByActiveTrueOrderByFullNameAsc();

    Optional<Employee> findByEmployeeCodeAndActiveTrue(String employeeCode);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);

    boolean existsByNationalId(String nationalId);

    boolean existsByPhoneAndIdNot(String phone, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByNationalIdAndIdNot(String nationalId, Long id);

    /** Active employee count, for the Dashboard's key statistics widget. */
    long countByActiveTrue();
}
