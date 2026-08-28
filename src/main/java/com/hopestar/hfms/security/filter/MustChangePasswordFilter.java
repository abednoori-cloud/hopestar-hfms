package com.hopestar.hfms.security.filter;

import com.hopestar.hfms.security.model.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Enforces the forced-password-change gate: while an authenticated user's
 * {@code User#mustChangePassword} is {@code true}, every request other
 * than the ones needed to actually change the password (or to log out) is
 * redirected to {@code /change-password}.
 * <p>
 * Reads the flag off {@link UserPrincipal#isMustChangePassword()} --
 * already resolved once at login time and held on the session's {@link
 * Authentication} principal -- rather than querying {@code users} on every
 * request. This mirrors how {@link UserPrincipal#isAccountNonLocked()} is
 * already used elsewhere in the security layer: computed once at login,
 * read from the session afterward. The one place the flag can go stale
 * mid-session -- the moment the password is actually changed -- is handled
 * by {@code AuthController} explicitly refreshing the session's {@code
 * Authentication} at that point, not by this filter re-querying the
 * database.
 */
public class MustChangePasswordFilter extends OncePerRequestFilter {

    /**
     * Exact paths reachable while a password change is pending: the change-
     * password screen itself (GET to render it, POST to submit it) and
     * logout, so a user who does not want to change their password right
     * now can still sign out instead of being stuck. Static assets and
     * error handling are excluded by prefix below rather than listed here.
     */
    private static final Set<String> ALLOWED_EXACT_PATHS = Set.of("/change-password", "/logout", "/error");

    private static final String[] ALLOWED_PATH_PREFIXES = {
            "/css/", "/js/", "/images/", "/static/", "/webjars/"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof UserPrincipal principal
                && principal.isMustChangePassword()
                && !isAllowed(request.getRequestURI(), request.getContextPath())) {
            response.sendRedirect(request.getContextPath() + "/change-password");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowed(String requestUri, String contextPath) {
        String path = contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)
                ? requestUri.substring(contextPath.length())
                : requestUri;

        if (ALLOWED_EXACT_PATHS.contains(path)) {
            return true;
        }
        for (String prefix : ALLOWED_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
