package com.hopestar.hfms.module.student.repository;

import com.hopestar.hfms.module.student.entity.StudentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentStatusRepository extends JpaRepository<StudentStatus, Long> {

    List<StudentStatus> findByActiveTrueOrderByNameAsc();

    Optional<StudentStatus> findByNameAndActiveTrue(String name);

    boolean existsByName(String name);
}
