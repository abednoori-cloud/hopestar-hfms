package com.hopestar.hfms.config;

import com.hopestar.hfms.common.util.SecurityUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * Enables Spring Data JPA auditing so {@code @CreatedDate},
 * {@code @LastModifiedDate}, {@code @CreatedBy} and {@code @LastModifiedBy}
 * on {@link com.hopestar.hfms.common.entity.BaseEntity} are populated
 * automatically on every entity in every module, per the approved
 * database design's uniform audit-column requirement (§2, general
 * principles).
 * <p>
 * This is distinct from {@link com.hopestar.hfms.audit.listener.AuditEntityListener},
 * which writes full before/after change records to {@code audit_logs};
 * this class only supplies the four audit *columns* on the entity itself.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of(SecurityUtil.currentUsername());
    }
}
