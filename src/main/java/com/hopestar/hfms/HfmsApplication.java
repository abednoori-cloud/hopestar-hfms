package com.hopestar.hfms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * HopeStar Finance & Student Management System (HFMS).
 * <p>
 * Entry point for the Spring Boot application. Task scheduling is enabled
 * via {@link com.hopestar.hfms.config.SchedulerConfig}, which also
 * provisions the dedicated thread pool used by the automatic backup,
 * salary-reminder and document-expiry jobs under
 * {@code com.hopestar.hfms.scheduler}.
 */
@SpringBootApplication
public class HfmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(HfmsApplication.class, args);
    }
}
