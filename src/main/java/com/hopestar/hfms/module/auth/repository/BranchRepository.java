package com.hopestar.hfms.module.auth.repository;

import com.hopestar.hfms.module.auth.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {

    Optional<Branch> findByBranchCodeAndActiveTrue(String branchCode);

    Optional<Branch> findFirstByHeadquartersTrueAndActiveTrue();

    boolean existsByBranchCode(String branchCode);
}
