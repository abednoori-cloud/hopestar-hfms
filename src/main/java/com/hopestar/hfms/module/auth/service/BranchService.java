package com.hopestar.hfms.module.auth.service;

import com.hopestar.hfms.module.auth.dto.BranchResponseDTO;

/**
 * Read-only access to {@link com.hopestar.hfms.module.auth.entity.Branch}
 * data. Today's only caller is the Receipt/Voucher PDF header, which needs
 * the headquarters branch's name/address/phone/email as the printed
 * "office info" -- there is no dedicated Settings module yet (see the
 * Module 2 dashboard/receipt inspection notes), so the existing Branch/HQ
 * record is the source of truth for this until one is built.
 */
public interface BranchService {

    /**
     * The headquarters branch (single-branch deployment today -- see
     * {@link com.hopestar.hfms.module.auth.entity.Branch}'s Javadoc).
     */
    BranchResponseDTO getHeadquarters();
}
