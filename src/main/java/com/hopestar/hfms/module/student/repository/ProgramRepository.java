package com.hopestar.hfms.module.student.repository;

import com.hopestar.hfms.module.student.entity.Program;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProgramRepository extends JpaRepository<Program, Long> {

    List<Program> findByActiveTrueOrderByNameAsc();

    List<Program> findByDestinationCountryIgnoreCaseAndActiveTrueOrderByNameAsc(String destinationCountry);

    Optional<Program> findByNameAndDestinationCountryAndActiveTrue(String name, String destinationCountry);

    boolean existsByNameAndDestinationCountry(String name, String destinationCountry);
}
