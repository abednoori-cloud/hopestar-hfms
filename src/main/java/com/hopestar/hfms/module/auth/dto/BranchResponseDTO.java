package com.hopestar.hfms.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Read-model for a {@link com.hopestar.hfms.module.auth.entity.Branch}.
 * Today's only consumer is the Receipt/Voucher PDF header (office name/
 * address/phone/email) -- kept as a small, generic branch read-model
 * rather than a PDF-specific DTO, so it stays reusable if a future need
 * (e.g. a proper Settings module) wants the same data.
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
}
