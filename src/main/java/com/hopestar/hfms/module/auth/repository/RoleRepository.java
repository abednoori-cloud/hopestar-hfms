package com.hopestar.hfms.module.auth.repository;

import com.hopestar.hfms.module.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByNameAndActiveTrue(String name);

    boolean existsByName(String name);
}
