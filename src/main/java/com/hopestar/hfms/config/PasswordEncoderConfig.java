package com.hopestar.hfms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Declares the {@link PasswordEncoder} bean in its own configuration
 * class, separate from {@link SecurityConfig}. {@code SecurityConfig}
 * depends (transitively, via {@code AuthService}) on {@code
 * AuthServiceImpl}, which itself depends on {@code PasswordEncoder} --
 * declaring that bean as an instance {@code @Bean} method on {@code
 * SecurityConfig} itself created an unresolvable circular reference
 * (Spring needs a fully-constructed {@code SecurityConfig} to invoke the
 * bean method, but {@code SecurityConfig} construction was blocked
 * waiting on {@code AuthServiceImpl}). Splitting it out here means
 * neither {@code SecurityConfig} nor {@code AuthServiceImpl} depends on
 * the other to obtain a {@code PasswordEncoder}.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Strength 12 per approved Security Architecture §5.1 — this is a
        // financial system and the default strength of 10 is intentionally
        // increased.
        return new BCryptPasswordEncoder(12);
    }
}
