package com.hopestar.hfms.module.finance.employee.repository;

import com.hopestar.hfms.module.finance.employee.entity.EmployeeStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeStatusRepository extends JpaRepository<EmployeeStatus, Long> {

    List<EmployeeStatus> findByActiveTrueOrderByNameAsc();

    Optional<EmployeeStatus> findByNameAndActiveTrue(String name);

    boolean existsByName(String name);
}
