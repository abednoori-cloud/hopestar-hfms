package com.hopestar.hfms.module.student.repository;

import com.hopestar.hfms.module.student.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * {@link JpaSpecificationExecutor} is included so {@code StudentService}
 * can compose a dynamic, multi-criteria search (name/code/phone/email/
 * program/country/status, any subset of which may be supplied) from a
 * {@code StudentSearchDTO} without hand-rolling a combinatorial explosion
 * of finder methods — while the explicit finders below remain for the
 * common single-field lookups (uniqueness checks, direct navigation).
 */
public interface StudentRepository extends JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {

    Optional<Student> findByIdAndActiveTrue(Long id);

    Optional<Student> findByStudentCodeAndActiveTrue(String studentCode);

    Optional<Student> findByPassportNumberAndActiveTrue(String passportNumber);

    Optional<Student> findByEmailAndActiveTrue(String email);

    boolean existsByStudentCode(String studentCode);

    boolean existsByPassportNumber(String passportNumber);

    boolean existsByEmail(String email);

    boolean existsByPassportNumberAndIdNot(String passportNumber, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    /** Active student count, for the Dashboard's key statistics widget. */
    long countByActiveTrue();

    /**
     * Active student count grouped by status name (e.g. ACTIVE, ON_HOLD,
     * COMPLETED) in a single query -- the Dashboard's "students by status"
     * statistic, per the task's explicit list, rather than one COUNT query
     * per status value.
     */
    @Query("SELECT s.status.name, COUNT(s) FROM Student s WHERE s.active = true GROUP BY s.status.name")
    List<Object[]> countActiveStudentsGroupedByStatus();
}
