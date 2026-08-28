package com.hopestar.hfms.audit.repository;

import com.hopestar.hfms.audit.entity.AuditLog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    Page<AuditLog> findByEntityNameAndEntityIdOrderByChangedAtDesc(String entityName, Long entityId, Pageable pageable);

    Page<AuditLog> findByEntityNameOrderByChangedAtDesc(String entityName, Pageable pageable);
}
