package com.hopestar.hfms.module.auth.controller;

import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.util.SecurityUtil;
import com.hopestar.hfms.module.auth.dto.ChangePasswordDTO;
import com.hopestar.hfms.module.auth.service.AuthService;
import com.hopestar.hfms.security.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * MVC controller for Module 1 (Authentication): the login page and the
 * forced/self-service change-password page. Thin per the module
 * architecture -- {@link AuthService} owns the actual account mutations
 * (failed-login tracking, password change); Spring Security's own form-
 * login filter (configured in {@code SecurityConfig}) owns the credential
 * check itself, so this controller never touches a password comparison
 * directly.
 */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CustomUserDetailsService customUserDetailsService;

    // ---------------------------------------------------------------
    // Login
    // ---------------------------------------------------------------

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                             @RequestParam(required = false) String logout,
                             @RequestParam(required = false) String locked,
                             @RequestParam(required = false) String expired,
                             Model model) {
        if (SecurityUtil.isAuthenticated()) {
            return "redirect:/dashboard";
        }
        if (locked != null) {
            model.addAttribute("errorMessage",
                    "This account has been locked due to too many failed login attempts. Please contact an administrator.");
        } else if (error != null) {
            model.addAttribute("errorMessage", "Invalid username or password.");
        } else if (expired != null) {
            model.addAttribute("errorMessage", "Your session has expired. Please log in again.");
        }
        if (logout != null) {
            model.addAttribute("successMessage", "You have been logged out successfully.");
        }
        return "auth/login";
    }

    // ---------------------------------------------------------------
    // Change Password (forced on must_change_password=true, or self-service)
    // ---------------------------------------------------------------

    @GetMapping("/change-password")
    public String changePasswordForm(Model model) {
        model.addAttribute("changePasswordDTO", new ChangePasswordDTO());
        return "auth/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(@Valid @ModelAttribute("changePasswordDTO") ChangePasswordDTO changePasswordDTO,
                                  BindingResult bindingResult,
                                  Authentication authentication,
                                  HttpServletRequest request,
                                  Model model,
                                  RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/change-password";
        }

        try {
            authService.changePassword(authentication.getName(), changePasswordDTO);
        } catch (BusinessValidationException ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/change-password";
        }

        refreshSessionPrincipal(authentication, request);

        redirectAttributes.addFlashAttribute("successMessage", "Your password has been changed successfully.");
        return "redirect:/dashboard";
    }

    /**
     * The {@code Authentication} held in this session was built at login
     * time from the user's <em>old</em> password hash and (now-stale)
     * {@code mustChangePassword=true} flag. Reloading the principal and
     * re-seeding both the thread-local {@link SecurityContextHolder} and
     * the session attribute Spring Security reads it back from on the
     * next request is what makes {@code mustChangePassword} flip to
     * {@code false} immediately -- in this same response -- instead of
     * requiring the user to log out and back in. This does not touch the
     * lockout/{@code accountNonLocked} handling at all; that continues to
     * be resolved the same way, once per login.
     */
    private void refreshSessionPrincipal(Authentication authentication, HttpServletRequest request) {
        UserDetails refreshed = customUserDetailsService.loadUserByUsername(authentication.getName());
        Authentication newAuth = new UsernamePasswordAuthenticationToken(
                refreshed, authentication.getCredentials(), refreshed.getAuthorities());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(newAuth);
        SecurityContextHolder.setContext(context);

        request.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }
}
