package com.hopestar.hfms.audit.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hopestar.hfms.audit.entity.AuditLog;
import com.hopestar.hfms.audit.repository.AuditLogRepository;
import com.hopestar.hfms.common.entity.BaseEntity;
import com.hopestar.hfms.common.enums.AuditAction;
import com.hopestar.hfms.common.util.SecurityUtil;
import com.hopestar.hfms.config.ApplicationContextProvider;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * JPA entity listener that records a row in {@code audit_logs} whenever any
 * {@link BaseEntity} subclass is created or updated (including
 * soft-deletes, since {@code softDelete()} triggers a normal JPA update).
 * <p>
 * Registered per-entity via {@code @EntityListeners(AuditEntityListener.class)}
 * in Phase 2 onward as feature-module entities are introduced; wired here
 * in Phase 1 so the mechanism exists before those entities land.
 * <p>
 * A full old-value/new-value diff (via Hibernate Envers or a similar
 * history mechanism) is out of scope for Phase 1 and noted as a follow-up;
 * for now this listener records the post-change snapshot of the entity
 * as {@code new_value}, which is sufficient for "who changed what record,
 * and when" traceability.
 */
@Slf4j
public class AuditEntityListener {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @PostPersist
    public void onPostPersist(Object entity) {
        record(entity, AuditAction.CREATE);
    }

    @PostUpdate
    public void onPostUpdate(Object entity) {
        AuditAction action = (entity instanceof BaseEntity base && !base.isActive())
                ? AuditAction.DELETE
                : AuditAction.UPDATE;
        record(entity, action);
    }

    private void record(Object entity, AuditAction action) {
        try {
            AuditLogRepository repository = ApplicationContextProvider.getBean(AuditLogRepository.class);
            if (repository == null || !(entity instanceof BaseEntity base)) {
                return;
            }
            AuditLog auditLog = AuditLog.builder()
                    .entityName(entity.getClass().getSimpleName())
                    .entityId(base.getId())
                    .action(action)
                    .newValue(OBJECT_MAPPER.writeValueAsString(entity))
                    .changedBy(SecurityUtil.currentUsername())
                    .changedAt(LocalDateTime.now())
                    .build();
            repository.save(auditLog);
        } catch (Exception ex) {
            // Auditing must never break the primary business transaction.
            log.error("Failed to write audit log for entity {}", entity.getClass().getSimpleName(), ex);
        }
    }
}
