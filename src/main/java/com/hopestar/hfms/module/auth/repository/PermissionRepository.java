package com.hopestar.hfms.module.auth.repository;

import com.hopestar.hfms.module.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByCodeAndActiveTrue(String code);

    boolean existsByCode(String code);
}
