package com.hopestar.hfms.config;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Exposes the Spring {@link ApplicationContext} statically so that classes
 * JPA instantiates itself — most notably
 * {@link com.hopestar.hfms.audit.listener.AuditEntityListener}, which is
 * registered via {@code @EntityListeners} and therefore is not a
 * Spring-managed bean and cannot use constructor injection — can still
 * reach Spring-managed beans (e.g. {@code AuditLogRepository}).
 * <p>
 * This is an intentionally narrow, well-documented exception to normal
 * dependency injection, used only where the JPA lifecycle leaves no other
 * option.
 */
@Component
public class ApplicationContextProvider implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextProvider.context = applicationContext;
    }

    public static <T> T getBean(Class<T> beanClass) {
        if (context == null) {
            return null;
        }
        return context.getBean(beanClass);
    }
}
