package com.hopestar.hfms.module.auth.service;

import com.hopestar.hfms.common.dto.StoredFileInfo;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.service.FileStorageService;
import com.hopestar.hfms.module.auth.dto.BranchResponseDTO;
import com.hopestar.hfms.module.auth.dto.OrganizationSettingsUpdateDTO;
import com.hopestar.hfms.module.auth.entity.Branch;
import com.hopestar.hfms.module.auth.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Implements {@link BranchService}. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;
    private final FileStorageService fileStorageService;

    @Override
    public BranchResponseDTO getHeadquarters() {
        return toResponseDTO(getHeadquartersEntity());
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public BranchResponseDTO updateOrganizationSettings(OrganizationSettingsUpdateDTO dto, MultipartFile logoFile) {
        Branch headquarters = getHeadquartersEntity();

        headquarters.setName(dto.getName());
        headquarters.setAddress(dto.getAddress());
        headquarters.setPhone(dto.getPhone());
        headquarters.setEmail(dto.getEmail());
        headquarters.setWebsite(dto.getWebsite());

        if (logoFile != null && !logoFile.isEmpty()) {
            String previousLogoPath = headquarters.getLogoPath();
            StoredFileInfo storedLogo = fileStorageService.storeDocument(logoFile, "branding");
            headquarters.setLogoPath(storedLogo.getStoredPath());
            if (previousLogoPath != null) {
                fileStorageService.delete(previousLogoPath);
            }
        }

        return toResponseDTO(headquarters);
    }

    // ---------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------

    private Branch getHeadquartersEntity() {
        return branchRepository.findFirstByHeadquartersTrueAndActiveTrue()
                .orElseThrow(() -> new BusinessValidationException("No headquarters branch is configured."));
    }

    private BranchResponseDTO toResponseDTO(Branch branch) {
        return BranchResponseDTO.builder()
                .id(branch.getId())
                .branchCode(branch.getBranchCode())
                .name(branch.getName())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .email(branch.getEmail())
                .website(branch.getWebsite())
                .logoPath(branch.getLogoPath())
                .build();
    }
}
