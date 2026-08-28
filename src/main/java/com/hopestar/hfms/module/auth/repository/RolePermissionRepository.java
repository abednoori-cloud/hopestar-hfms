package com.hopestar.hfms.module.auth.repository;

import com.hopestar.hfms.module.auth.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {

    List<RolePermission> findByRoleIdAndActiveTrue(Long roleId);

    boolean existsByRoleIdAndPermissionId(Long roleId, Long permissionId);
}
