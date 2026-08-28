package com.hopestar.hfms.module.auth.controller;

import com.hopestar.hfms.config.FileStorageProperties;
import com.hopestar.hfms.config.SecurityConfig;
import com.hopestar.hfms.module.auth.entity.Role;
import com.hopestar.hfms.module.auth.entity.User;
import com.hopestar.hfms.module.auth.service.AuthService;
import com.hopestar.hfms.module.dashboard.controller.DashboardController;
import com.hopestar.hfms.module.dashboard.dto.DashboardSummaryDTO;
import com.hopestar.hfms.module.dashboard.dto.FinancialSummaryDTO;
import com.hopestar.hfms.module.dashboard.dto.OutstandingReceivablesDTO;
import com.hopestar.hfms.module.dashboard.dto.SalaryOverviewDTO;
import com.hopestar.hfms.module.dashboard.dto.StudentEmployeeStatsDTO;
import com.hopestar.hfms.module.dashboard.service.DashboardService;
import com.hopestar.hfms.security.model.UserPrincipal;
import com.hopestar.hfms.security.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Web-layer tests for Module 1 (Authentication), run through the real
 * {@link SecurityConfig} filter chain ({@code @Import}ed rather than
 * re-created here) so form login, the failure/success handlers, the
 * account-lockout check, the forced-password-change gate, and logout are
 * all exercised the way they actually run in production -- only {@link
 * CustomUserDetailsService} (the DB-backed lookup) and {@link AuthService}
 * (the DB-backed mutations) are mocked, since no database is available in
 * this environment.
 * <p>
 * {@code @WebMvcTest} auto-detects {@code WebConfig} (a {@code
 * WebMvcConfigurer} bean, one of the component types this test slice picks
 * up on its own), which requires a {@link FileStorageProperties} bean.
 * {@code @EnableConfigurationProperties} registers just that properties
 * bean with its defaults -- unlike importing {@code FileStorageConfig}
 * itself, this does not run its {@code @PostConstruct} storage-directory
 * creation, which has nothing to do with what this test class covers.
 */
@WebMvcTest(controllers = {AuthController.class, DashboardController.class})
@Import(SecurityConfig.class)
@EnableConfigurationProperties(FileStorageProperties.class)
class AuthControllerTest {

    private static final String USERNAME = "jdoe";
    private static final String RAW_PASSWORD = "CorrectPass123";
    private static final String ENCODED_PASSWORD = new BCryptPasswordEncoder(12).encode(RAW_PASSWORD);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AuthService authService;

    /**
     * {@code @WebMvcTest} loads {@link DashboardController} alongside
     * {@link AuthController} (both were already under test here for the
     * authentication redirect/gate assertions below), and {@code
     * DashboardController} now depends on {@link DashboardService}
     * (Module 2) -- mocked here purely so this context can start; only
     * {@link #protectedPage_whenAuthenticated_succeeds()} actually renders
     * the dashboard view and needs a real stubbed return value.
     */
    @MockBean
    private DashboardService dashboardService;

    private DashboardSummaryDTO minimalDashboardSummary() {
        return DashboardSummaryDTO.builder()
                .financialSummary(FinancialSummaryDTO.builder()
                        .totalIncomeUsd(BigDecimal.ZERO).totalExpenseUsd(BigDecimal.ZERO)
                        .netPositionUsd(BigDecimal.ZERO).currentPeriodIncomeUsd(BigDecimal.ZERO)
                        .currentPeriodExpenseUsd(BigDecimal.ZERO).currentPeriodNetUsd(BigDecimal.ZERO)
                        .currentPeriodLabel("August 2026").build())
                .outstandingReceivables(OutstandingReceivablesDTO.builder()
                        .totalOutstandingUsd(BigDecimal.ZERO).studentsWithOutstandingBalanceCount(0)
                        .topOutstandingContracts(List.of()).build())
                .salaryOverview(SalaryOverviewDTO.builder()
                        .pendingSalaryCount(0).pendingBasicPayUsd(BigDecimal.ZERO)
                        .currentPeriodTotalCount(0).currentPeriodDraftCount(0).currentPeriodPostedCount(0)
                        .currentPeriodLabel("August 2026").pendingSalaries(List.of()).build())
                .recentTransactions(List.of())
                .stats(StudentEmployeeStatsDTO.builder()
                        .activeStudentCount(0).activeEmployeeCount(0)
                        .activeStudentsByStatus(Map.of()).build())
                .build();
    }

    private UserPrincipal principal(boolean locked, boolean mustChangePassword) {
        // .active(true) is set explicitly here because BaseEntity.active's
        // plain field initializer is silently ignored by Lombok's
        // @SuperBuilder (it needs @Builder.Default to take effect) --
        // relying on the builder's default would produce active=false,
        // making DaoAuthenticationProvider treat this user as disabled.
        // See the accompanying report for the app-wide implications of
        // this outside Module 1's scope; not fixed here.
        User user = User.builder()
                .id(1L)
                .username(USERNAME)
                .passwordHash(ENCODED_PASSWORD)
                .fullName("Jane Doe")
                .role(Role.builder().id(1L).name("ADMIN").build())
                .active(true)
                .accountLocked(locked)
                .mustChangePassword(mustChangePassword)
                .build();
        return new UserPrincipal(user, Set.of("STUDENT_VIEW"));
    }

    // ---------------------------------------------------------------
    // Login
    // ---------------------------------------------------------------

    @Test
    void loginPage_unauthenticated_rendersLoginView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void loginSuccess_redirectsToDashboard_andResetsFailedAttempts() throws Exception {
        when(customUserDetailsService.loadUserByUsername(USERNAME)).thenReturn(principal(false, false));

        mockMvc.perform(formLogin().user(USERNAME).password(RAW_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(authService).recordSuccessfulLogin(USERNAME);
    }

    @Test
    void loginSuccess_withMustChangePassword_redirectsToChangePassword() throws Exception {
        when(customUserDetailsService.loadUserByUsername(USERNAME)).thenReturn(principal(false, true));

        mockMvc.perform(formLogin().user(USERNAME).password(RAW_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/change-password"));
    }

    @Test
    void loginFailure_badCredentials_redirectsWithError_andRecordsFailedAttempt() throws Exception {
        when(customUserDetailsService.loadUserByUsername(USERNAME)).thenReturn(principal(false, false));
        when(authService.recordFailedLoginAttempt(USERNAME)).thenReturn(false);

        mockMvc.perform(formLogin().user(USERNAME).password("wrongPassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));

        verify(authService).recordFailedLoginAttempt(USERNAME);
        verify(authService, never()).recordSuccessfulLogin(any());
    }

    @Test
    void loginFailure_attemptThatTripsThreshold_redirectsWithLocked() throws Exception {
        when(customUserDetailsService.loadUserByUsername(USERNAME)).thenReturn(principal(false, false));
        when(authService.recordFailedLoginAttempt(USERNAME)).thenReturn(true);

        mockMvc.perform(formLogin().user(USERNAME).password("wrongPassword"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?locked=true"));
    }

    @Test
    void loginFailure_alreadyLockedAccount_redirectsWithLocked_withoutDoubleCounting() throws Exception {
        when(customUserDetailsService.loadUserByUsername(USERNAME)).thenReturn(principal(true, false));

        mockMvc.perform(formLogin().user(USERNAME).password(RAW_PASSWORD))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?locked=true"));

        verify(authService, never()).recordFailedLoginAttempt(any());
    }

    // ---------------------------------------------------------------
    // Authorization gate
    // ---------------------------------------------------------------

    @Test
    void protectedPage_whenUnauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void protectedPage_whenAuthenticated_succeeds() throws Exception {
        when(dashboardService.getDashboardSummary()).thenReturn(minimalDashboardSummary());

        mockMvc.perform(get("/dashboard").with(user(principal(false, false))))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"));
    }

    @Test
    void protectedPage_whenMustChangePasswordSet_isRedirectedToChangePassword_notServed() throws Exception {
        mockMvc.perform(get("/dashboard").with(user(principal(false, true))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/change-password"));
    }

    @Test
    void changePasswordForm_remainsReachable_whileMustChangePasswordIsSet() throws Exception {
        mockMvc.perform(get("/change-password").with(user(principal(false, true))))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/change-password"));
    }

    // ---------------------------------------------------------------
    // Change password
    // ---------------------------------------------------------------

    @Test
    void changePassword_success_redirectsToDashboard() throws Exception {
        when(customUserDetailsService.loadUserByUsername(USERNAME)).thenReturn(principal(false, false));

        mockMvc.perform(post("/change-password")
                        .with(user(principal(false, true)))
                        .with(csrf())
                        .param("currentPassword", RAW_PASSWORD)
                        .param("newPassword", "BrandNewPass123")
                        .param("confirmPassword", "BrandNewPass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));

        verify(authService).changePassword(eq(USERNAME), any());
    }

    @Test
    void changePassword_blankFields_reRendersForm_withoutCallingService() throws Exception {
        mockMvc.perform(post("/change-password")
                        .with(user(principal(false, true)))
                        .with(csrf())
                        .param("currentPassword", "")
                        .param("newPassword", "")
                        .param("confirmPassword", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/change-password"));

        verify(authService, never()).changePassword(any(), any());
    }

    // ---------------------------------------------------------------
    // Logout
    // ---------------------------------------------------------------

    @Test
    void logout_invalidatesSessionAndRedirectsToLoginWithLogoutFlag() throws Exception {
        // LogoutRequestBuilder (SecurityMockMvcRequestBuilders.logout()) has
        // no .with(RequestPostProcessor) overload in this Spring Security
        // version, so the authenticated session is set up the same way
        // logout() would build it internally: a POST to /logout with CSRF.
        mockMvc.perform(post("/logout").with(user(principal(false, false))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }
}
