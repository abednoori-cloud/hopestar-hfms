package com.hopestar.hfms.module.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Input for the Organization Settings form, which edits the headquarters
 * {@link com.hopestar.hfms.module.auth.entity.Branch} record in place. The
 * logo, if replaced, travels as a separate {@code MultipartFile} controller
 * parameter, not as part of this DTO -- mirrors {@code
 * StudentDocumentUploadDTO}'s precedent for metadata-plus-file forms.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationSettingsUpdateDTO {

    @NotBlank(message = "Organization name is required")
    @Size(max = 150, message = "Organization name cannot exceed 150 characters")
    private String name;

    @Size(max = 255, message = "Address cannot exceed 255 characters")
    private String address;

    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    private String phone;

    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    @Size(max = 255, message = "Website cannot exceed 255 characters")
    private String website;
}
