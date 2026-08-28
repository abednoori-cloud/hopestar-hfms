package com.hopestar.hfms.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Input for the forced/self-service password change flow (Module 1). The
 * cross-field checks (current password matches, new password/confirmation
 * match, new password differs from the current one) are business rules,
 * not shape validation, so per this codebase's convention they are
 * enforced in {@code AuthServiceImpl}, not here — mirrors how {@code
 * StudentContractServiceImpl} validates discount-vs-total in the service
 * layer rather than via a cross-field Bean Validation annotation.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordDTO {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 8, max = 100, message = "New password must be at least 8 characters")
    private String newPassword;

    @NotBlank(message = "Please confirm the new password")
    private String confirmPassword;
}
