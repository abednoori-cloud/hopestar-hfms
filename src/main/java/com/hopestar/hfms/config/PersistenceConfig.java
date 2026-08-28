package com.hopestar.hfms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Explicit JPA repository scanning and transaction-management activation.
 * <p>
 * Spring Boot's auto-configuration would enable both of these implicitly,
 * but they are declared explicitly here — scoped to the
 * {@code com.hopestar.hfms} root package, which covers every current and
 * future {@code module.*.repository} package (per the approved package
 * structure §3) — so the persistence layer's configuration is visible and
 * intentional rather than "magic," and so repository scanning does not
 * silently change if the application's base package ever moves.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.hopestar.hfms")
@EnableTransactionManagement
public class PersistenceConfig {
}
