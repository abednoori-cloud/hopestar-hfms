package com.hopestar.hfms.common.util;

import lombok.experimental.UtilityClass;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Convenience accessor for the currently authenticated principal's
 * username, used by {@code AuditConfig}'s {@code AuditorAware} bean and by
 * services that need to stamp "performed by" fields (e.g. expense
 * approvals, backup triggers) outside of the standard
 * created_by/updated_by columns already handled by {@link
 * com.hopestar.hfms.common.entity.BaseEntity}.
 */
@UtilityClass
public class SecurityUtil {

    public final String SYSTEM_USER = "system";

    public String currentUsername() {
        return currentAuthentication()
                .map(Authentication::getName)
                .orElse(SYSTEM_USER);
    }

    public Optional<Authentication> currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        return Optional.of(authentication);
    }

    public boolean isAuthenticated() {
        return currentAuthentication().isPresent();
    }
}
