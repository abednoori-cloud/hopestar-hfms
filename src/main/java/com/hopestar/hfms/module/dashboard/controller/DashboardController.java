package com.hopestar.hfms.module.dashboard.controller;

import com.hopestar.hfms.module.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * MVC controller for Module 2 (Dashboard). Thin per the module
 * architecture -- all aggregation lives in {@link DashboardService}; this
 * class only invokes it and hands the resulting view-model to the
 * template. Reachable only by an authenticated user: {@code
 * SecurityConfig}'s {@code anyRequest().authenticated()} rule already
 * covers {@code /dashboard} (it is not in the permitAll list), and this
 * is also the page every successful login lands on, so no further
 * authorization check is added here -- it is the universal post-login
 * landing page, not a privileged action.
 */
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("summary", dashboardService.getDashboardSummary());
        return "dashboard/index";
    }
}
