package com.hopestar.hfms.module.auth.service;

import com.hopestar.hfms.module.auth.dto.BranchResponseDTO;
import com.hopestar.hfms.module.auth.dto.OrganizationSettingsUpdateDTO;
import org.springframework.web.multipart.MultipartFile;

/**
 * Read/write access to {@link com.hopestar.hfms.module.auth.entity.Branch}
 * data. The Receipt/Voucher/Report PDF header (name/address/phone/email)
 * and the Organization Settings page both read through {@link
 * #getHeadquarters()} -- there is still no separate Settings/Organization
 * entity; the existing Branch/HQ record is the source of truth, per the
 * design intent this interface already documented before the Settings
 * page existed.
 */
public interface BranchService {

    /**
     * The headquarters branch (single-branch deployment today -- see
     * {@link com.hopestar.hfms.module.auth.entity.Branch}'s Javadoc).
     */
    BranchResponseDTO getHeadquarters();

    /**
     * Updates the headquarters branch's editable Organization Settings
     * fields (name/address/phone/email/website) and, if {@code logoFile}
     * is non-null and non-empty, stores it via {@code FileStorageService}
     * and replaces {@code logoPath} -- deleting the previously stored logo
     * file, if any, so uploads don't accumulate orphaned files.
     */
    BranchResponseDTO updateOrganizationSettings(OrganizationSettingsUpdateDTO dto, MultipartFile logoFile);
}
