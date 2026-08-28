package com.hopestar.hfms.module.auth.service;

import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.module.auth.dto.ChangePasswordDTO;
import com.hopestar.hfms.module.auth.entity.User;
import com.hopestar.hfms.module.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the account-lockout and password-change logic backing
 * Module 1 (Authentication). Deliberately does not start a Spring context
 * or touch a database -- this exercises {@link AuthServiceImpl}'s own
 * decision logic (which wraps {@link User#registerFailedLogin(int)} /
 * {@link User#registerSuccessfulLogin()}) in isolation, the same logic
 * {@code SecurityConfig}'s authentication success/failure handlers
 * delegate to.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final int MAX_ATTEMPTS = 5;
    private static final String USERNAME = "jdoe";
    private static final String OLD_HASH = "{bcrypt}old-hash";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(authService, "maxFailedLoginAttempts", MAX_ATTEMPTS);
    }

    private User activeUser() {
        return User.builder()
                .id(1L)
                .username(USERNAME)
                .passwordHash(OLD_HASH)
                .fullName("Jane Doe")
                .failedLoginAttempts(0)
                .accountLocked(false)
                .mustChangePassword(false)
                .build();
    }

    // ---------------------------------------------------------------
    // Failed login / lockout
    // ---------------------------------------------------------------

    @Test
    void recordFailedLoginAttempt_incrementsCounter_belowThreshold() {
        User user = activeUser();
        user.setFailedLoginAttempts(2);
        when(userRepository.findByUsernameAndActiveTrue(USERNAME)).thenReturn(Optional.of(user));

        boolean justLocked = authService.recordFailedLoginAttempt(USERNAME);

        assertThat(justLocked).isFalse();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(3);
        assertThat(user.isAccountLocked()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void recordFailedLoginAttempt_locksAccount_whenThresholdReached() {
        User user = activeUser();
        user.setFailedLoginAttempts(MAX_ATTEMPTS - 1);
        when(userRepository.findByUsernameAndActiveTrue(USERNAME)).thenReturn(Optional.of(user));

        boolean justLocked = authService.recordFailedLoginAttempt(USERNAME);

        assertThat(justLocked).isTrue();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(user.isAccountLocked()).isTrue();
    }

    @Test
    void recordFailedLoginAttempt_onAlreadyLockedAccount_doesNotDoubleCountOrSave() {
        User user = activeUser();
        user.setFailedLoginAttempts(MAX_ATTEMPTS);
        user.setAccountLocked(true);
        when(userRepository.findByUsernameAndActiveTrue(USERNAME)).thenReturn(Optional.of(user));

        boolean justLocked = authService.recordFailedLoginAttempt(USERNAME);

        assertThat(justLocked).isFalse();
        assertThat(user.getFailedLoginAttempts()).isEqualTo(MAX_ATTEMPTS);
        verify(userRepository, never()).save(any());
    }

    @Test
    void recordFailedLoginAttempt_unknownUsername_doesNothing_neverRevealsExistence() {
        when(userRepository.findByUsernameAndActiveTrue("ghost")).thenReturn(Optional.empty());

        boolean justLocked = authService.recordFailedLoginAttempt("ghost");

        assertThat(justLocked).isFalse();
        verify(userRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // Successful login
    // ---------------------------------------------------------------

    @Test
    void recordSuccessfulLogin_resetsCounterAndUnlocksAccount() {
        User user = activeUser();
        user.setFailedLoginAttempts(4);
        when(userRepository.findByUsernameAndActiveTrue(USERNAME)).thenReturn(Optional.of(user));

        authService.recordSuccessfulLogin(USERNAME);

        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.isAccountLocked()).isFalse();
        assertThat(user.getLastLoginAt()).isNotNull();
        verify(userRepository).save(user);
    }

    // ---------------------------------------------------------------
    // Change password
    // ---------------------------------------------------------------

    @Test
    void changePassword_success_encodesNewHashAndClearsMustChangeFlag() {
        User user = activeUser();
        user.setMustChangePassword(true);
        when(userRepository.findByUsernameAndActiveTrue(USERNAME)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass123", OLD_HASH)).thenReturn(true);
        when(passwordEncoder.matches("NewPass123", OLD_HASH)).thenReturn(false);
        when(passwordEncoder.encode("NewPass123")).thenReturn("{bcrypt}new-hash");

        authService.changePassword(USERNAME, new ChangePasswordDTO("oldPass123", "NewPass123", "NewPass123"));

        assertThat(user.getPasswordHash()).isEqualTo("{bcrypt}new-hash");
        assertThat(user.isMustChangePassword()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_wrongCurrentPassword_throwsAndDoesNotSave() {
        User user = activeUser();
        when(userRepository.findByUsernameAndActiveTrue(USERNAME)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", OLD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(USERNAME,
                new ChangePasswordDTO("wrongPass", "NewPass123", "NewPass123")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("Current password is incorrect");
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_mismatchedConfirmation_throws() {
        User user = activeUser();
        when(userRepository.findByUsernameAndActiveTrue(USERNAME)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass123", OLD_HASH)).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword(USERNAME,
                new ChangePasswordDTO("oldPass123", "NewPass123", "Different123")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("do not match");
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_sameAsCurrentPassword_throws() {
        User user = activeUser();
        when(userRepository.findByUsernameAndActiveTrue(USERNAME)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("oldPass123", OLD_HASH)).thenReturn(true);

        assertThatThrownBy(() -> authService.changePassword(USERNAME,
                new ChangePasswordDTO("oldPass123", "oldPass123", "oldPass123")))
                .isInstanceOf(BusinessValidationException.class)
                .hasMessageContaining("different from the current password");
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_unknownUser_throwsResourceNotFound() {
        when(userRepository.findByUsernameAndActiveTrue("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.changePassword("ghost",
                new ChangePasswordDTO("a", "NewPass123", "NewPass123")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
