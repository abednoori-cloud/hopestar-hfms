package com.hopestar.hfms.config;

import com.hopestar.hfms.module.auth.service.AuthService;
import com.hopestar.hfms.security.filter.MustChangePasswordFilter;
import com.hopestar.hfms.security.model.UserPrincipal;
import com.hopestar.hfms.security.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.util.StringUtils;

/**
 * Central Spring Security configuration for HFMS.
 * <p>
 * Session-based form login is used (appropriate for a server-rendered
 * Thymeleaf app, per the approved Security Architecture §5.1) with CSRF
 * protection enabled by default — Thymeleaf's
 * {@code thymeleaf-extras-springsecurity6} dialect automatically injects
 * CSRF tokens into {@code <form>} elements, so no template needs to
 * reference the token manually.
 * <p>
 * Only the ADMIN role exists at go-live, but {@code @EnableMethodSecurity}
 * is turned on now so {@code @PreAuthorize} checks can be added to Service
 * methods as each module is implemented, per §5.2 of the approved
 * architecture ("defense in depth" — authorization checked at the service
 * layer, not only at the URL layer).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final AuthService authService;
    // Declared in PasswordEncoderConfig, not here -- see that class's
    // Javadoc for why: an instance @Bean method for PasswordEncoder on
    // this class created a circular reference through AuthServiceImpl.
    private final PasswordEncoder passwordEncoder;

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public org.springframework.security.authentication.AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        // Required for Spring Security's concurrent-session control below
        // to be notified when sessions are destroyed.
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/login",
                                "/css/**", "/js/**", "/images/**", "/static/**",
                                "/webjars/**", "/favicon.ico",
                                "/error"
                        ).permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Every other request requires authentication. Fine-
                        // grained per-module authorization (e.g. only ADMIN
                        // can access /settings/backup) is layered on with
                        // @PreAuthorize at the service layer as each module
                        // is implemented, rather than duplicated here as a
                        // long list of URL rules that would drift out of
                        // sync with the service-layer checks.
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(authenticationSuccessHandler())
                        .failureHandler(authenticationFailureHandler())
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.newSession())
                        .maximumSessions(1)
                        .expiredUrl("/login?expired=true")
                )
                // CSRF protection stays enabled (default) for every state-
                // changing endpoint per approved Security Architecture §5.3
                // ("no financial state changes via GET" and CSRF-token-
                // protected POST/PUT forms).
                .headers(headers -> headers
                        .contentTypeOptions(contentTypeOptionsConfig -> {})
                        .frameOptions(frameOptionsConfig -> frameOptionsConfig.sameOrigin())
                )
                // Runs once per request, after the user is authenticated,
                // to enforce the forced-password-change gate (Module 1).
                // See MustChangePasswordFilter's Javadoc for why this reads
                // the session principal rather than querying the database.
                .addFilterAfter(mustChangePasswordFilter(), UsernamePasswordAuthenticationFilter.class);

        http.authenticationProvider(authenticationProvider());

        return http.build();
    }

    @Bean
    public MustChangePasswordFilter mustChangePasswordFilter() {
        return new MustChangePasswordFilter();
    }

    /**
     * On successful authentication: resets the failed-login counter (via
     * {@link AuthService#recordSuccessfulLogin}, the same {@code
     * User#registerSuccessfulLogin()} used by the lockout mechanism), then
     * redirects to the change-password page if {@link
     * UserPrincipal#isMustChangePassword()} is set, or {@code /dashboard}
     * otherwise.
     */
    private AuthenticationSuccessHandler authenticationSuccessHandler() {
        return (request, response, authentication) -> {
            authService.recordSuccessfulLogin(authentication.getName());
            boolean mustChangePassword = authentication.getPrincipal() instanceof UserPrincipal principal
                    && principal.isMustChangePassword();
            response.sendRedirect(mustChangePassword ? "/change-password" : "/dashboard");
        };
    }

    /**
     * On failed authentication: increments the failed-login counter via
     * {@link AuthService#recordFailedLoginAttempt} -- but only for a
     * genuine wrong-password attempt ({@link
     * org.springframework.security.authentication.BadCredentialsException}),
     * never for an already-locked account ({@link LockedException}, thrown
     * by {@code DaoAuthenticationProvider}'s pre-authentication check
     * before the password is even compared) -- which is what keeps the
     * failed-login logic in exactly one place ({@code
     * AuthServiceImpl#recordFailedLoginAttempt}) instead of being
     * duplicated here. Redirects with a distinct query parameter per
     * failure reason so the login page can show an appropriate message,
     * without ever revealing whether a given username exists (Spring
     * Security's default {@code hideUserNotFoundExceptions} already maps
     * an unknown username to the same {@code BadCredentialsException} as a
     * wrong password).
     */
    private AuthenticationFailureHandler authenticationFailureHandler() {
        return (request, response, exception) -> {
            if (exception instanceof LockedException) {
                response.sendRedirect("/login?locked=true");
                return;
            }
            String username = request.getParameter("username");
            boolean justLocked = StringUtils.hasText(username) && authService.recordFailedLoginAttempt(username);
            response.sendRedirect(justLocked ? "/login?locked=true" : "/login?error=true");
        };
    }
}
