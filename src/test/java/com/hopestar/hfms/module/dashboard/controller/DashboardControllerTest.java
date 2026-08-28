package com.hopestar.hfms.module.dashboard.controller;

import com.hopestar.hfms.config.FileStorageProperties;
import com.hopestar.hfms.config.SecurityConfig;
import com.hopestar.hfms.module.auth.entity.Role;
import com.hopestar.hfms.module.auth.entity.User;
import com.hopestar.hfms.module.auth.service.AuthService;
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
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Web-layer tests for Module 2 (Dashboard), run through the real {@link
 * SecurityConfig} filter chain -- verifies the authentication requirement
 * from Module 1 still applies to {@code /dashboard} and that a real
 * summary reaches the view. {@link DashboardService} is mocked so this
 * test never touches a repository or a database.
 */
@WebMvcTest(controllers = DashboardController.class)
@Import(SecurityConfig.class)
@EnableConfigurationProperties(FileStorageProperties.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @MockBean
    private AuthService authService;

    @MockBean
    private DashboardService dashboardService;

    private UserPrincipal principal() {
        User user = User.builder()
                .id(1L)
                .username("jdoe")
                .passwordHash("{bcrypt}hash")
                .fullName("Jane Doe")
                .role(Role.builder().id(1L).name("ADMIN").build())
                .active(true)
                .accountLocked(false)
                .mustChangePassword(false)
                .build();
        return new UserPrincipal(user, Set.of("STUDENT_VIEW"));
    }

    private DashboardSummaryDTO stubSummary() {
        return DashboardSummaryDTO.builder()
                .financialSummary(FinancialSummaryDTO.builder()
                        .totalIncomeUsd(BigDecimal.ZERO)
                        .totalExpenseUsd(BigDecimal.ZERO)
                        .netPositionUsd(BigDecimal.ZERO)
                        .currentPeriodIncomeUsd(BigDecimal.ZERO)
                        .currentPeriodExpenseUsd(BigDecimal.ZERO)
                        .currentPeriodNetUsd(BigDecimal.ZERO)
                        .currentPeriodLabel("August 2026")
                        .build())
                .outstandingReceivables(OutstandingReceivablesDTO.builder()
                        .totalOutstandingUsd(BigDecimal.ZERO)
                        .studentsWithOutstandingBalanceCount(0)
                        .topOutstandingContracts(List.of())
                        .build())
                .salaryOverview(SalaryOverviewDTO.builder()
                        .pendingSalaryCount(0)
                        .pendingBasicPayUsd(BigDecimal.ZERO)
                        .currentPeriodTotalCount(0)
                        .currentPeriodDraftCount(0)
                        .currentPeriodPostedCount(0)
                        .currentPeriodLabel("August 2026")
                        .pendingSalaries(List.of())
                        .build())
                .recentTransactions(List.of())
                .stats(StudentEmployeeStatsDTO.builder()
                        .activeStudentCount(0)
                        .activeEmployeeCount(0)
                        .activeStudentsByStatus(Map.of())
                        .build())
                .build();
    }

    @Test
    void dashboard_whenUnauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void dashboard_whenAuthenticated_rendersWithSummary() throws Exception {
        when(dashboardService.getDashboardSummary()).thenReturn(stubSummary());

        mockMvc.perform(get("/dashboard").with(user(principal())))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"))
                .andExpect(model().attributeExists("summary"));
    }
}
