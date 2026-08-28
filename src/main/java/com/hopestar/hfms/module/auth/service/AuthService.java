package com.hopestar.hfms.module.auth.service;

import com.hopestar.hfms.module.auth.dto.ChangePasswordDTO;

/**
 * Authentication-adjacent account operations for Module 1 (Authentication):
 * failed-login tracking (backing the account-lockout mechanism already
 * modeled on {@link com.hopestar.hfms.module.auth.entity.User}) and
 * self-service/forced password changes. Deliberately does not duplicate
 * {@link com.hopestar.hfms.security.service.CustomUserDetailsService}'s
 * job of loading a user for Spring Security -- this service only mutates
 * account state around the authentication event.
 */
public interface AuthService {

    /**
     * Called by the {@code AuthenticationSuccessHandler} after Spring
     * Security has already verified the password. Resets the failed-login
     * counter and clears any lock, per {@code User#registerSuccessfulLogin()}.
     */
    void recordSuccessfulLogin(String username);

    /**
     * Called by the {@code AuthenticationFailureHandler} on a wrong-password
     * failure (never on an already-locked account -- see the handler for
     * why). Increments the failed-login counter and locks the account once
     * {@code hfms.security.max-failed-login-attempts} is reached, per
     * {@code User#registerFailedLogin(int)}. Silently does nothing (and
     * returns {@code false}) if the username does not correspond to an
     * active account, so this method never reveals account existence to a
     * caller.
     *
     * @return {@code true} if this specific attempt is the one that just
     *         locked the account, so the caller can show "account locked"
     *         immediately rather than a generic "wrong password" on the
     *         very request that tripped the threshold.
     */
    boolean recordFailedLoginAttempt(String username);

    /**
     * Changes the given user's password: verifies {@code currentPassword}
     * against the stored BCrypt hash, verifies {@code newPassword} matches
     * {@code confirmPassword} and differs from the current password,
     * encodes and persists the new hash, and clears {@code
     * mustChangePassword}. Throws {@link
     * com.hopestar.hfms.common.exception.BusinessValidationException} on
     * any of those checks failing.
     */
    void changePassword(String username, ChangePasswordDTO dto);
}
