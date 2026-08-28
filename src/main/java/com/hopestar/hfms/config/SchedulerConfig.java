package com.hopestar.hfms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Dedicated thread pool for the application's scheduled jobs (automatic
 * backups, upcoming-salary reminders, document-expiry checks — see
 * {@code com.hopestar.hfms.scheduler}, approved architecture §3/§7.1),
 * so background jobs never contend with the default single-threaded
 * scheduler or with request-handling threads.
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("hfms-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setErrorHandler(throwable ->
                org.slf4j.LoggerFactory.getLogger(SchedulerConfig.class)
                        .error("Scheduled task failed", throwable));
        scheduler.initialize();
        return scheduler;
    }
}
