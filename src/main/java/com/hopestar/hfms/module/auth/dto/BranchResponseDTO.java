package com.hopestar.hfms.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Read-model for a {@link com.hopestar.hfms.module.auth.entity.Branch}.
 * Originally built for just the Receipt/Voucher/Report PDF header (name/
 * address/phone/email); {@link #website}/{@link #logoPath} were added for
 * the Organization Settings page, which is exactly the "future need" this
 * DTO's own earlier Javadoc anticipated reusing it for.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchResponseDTO {

    private Long id;
    private String branchCode;
    private String name;
    private String address;
    private String phone;
    private String email;
    private String website;
    private String logoPath;
}
