package com.hopestar.hfms.module.auth.service;

import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.module.auth.dto.ChangePasswordDTO;
import com.hopestar.hfms.module.auth.entity.User;
import com.hopestar.hfms.module.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implements {@link AuthService}. See that interface's Javadoc for the
 * governing business rules.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${hfms.security.max-failed-login-attempts}")
    private int maxFailedLoginAttempts;

    @Override
    @Transactional
    public void recordSuccessfulLogin(String username) {
        userRepository.findByUsernameAndActiveTrue(username).ifPresent(user -> {
            user.registerSuccessfulLogin();
            userRepository.save(user);
        });
    }

    @Override
    @Transactional
    public boolean recordFailedLoginAttempt(String username) {
        return userRepository.findByUsernameAndActiveTrue(username)
                .map(user -> {
                    // An already-locked account short-circuits at Spring
                    // Security's pre-authentication check (LockedException)
                    // before the password is even compared, so this branch
                    // only ever runs for a genuine wrong-password attempt
                    // against a still-unlocked account -- never double-
                    // counts an attempt against an already-locked one.
                    if (user.isAccountLocked()) {
                        return false;
                    }
                    user.registerFailedLogin(maxFailedLoginAttempts);
                    userRepository.save(user);
                    return user.isAccountLocked();
                })
                .orElse(false);
    }

    @Override
    @Transactional
    public void changePassword(String username, ChangePasswordDTO dto) {
        User user = userRepository.findByUsernameAndActiveTrue(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", username));

        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
            throw new BusinessValidationException("Current password is incorrect.");
        }
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessValidationException("New password and confirmation do not match.");
        }
        if (passwordEncoder.matches(dto.getNewPassword(), user.getPasswordHash())) {
            throw new BusinessValidationException("New password must be different from the current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        user.setMustChangePassword(false);
        userRepository.save(user);
    }
}
