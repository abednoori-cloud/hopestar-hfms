package com.hopestar.hfms.module.auth.service;

import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.module.auth.dto.BranchResponseDTO;
import com.hopestar.hfms.module.auth.entity.Branch;
import com.hopestar.hfms.module.auth.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Implements {@link BranchService}. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;

    @Override
    public BranchResponseDTO getHeadquarters() {
        Branch headquarters = branchRepository.findFirstByHeadquartersTrueAndActiveTrue()
                .orElseThrow(() -> new BusinessValidationException("No headquarters branch is configured."));

        return BranchResponseDTO.builder()
                .id(headquarters.getId())
                .branchCode(headquarters.getBranchCode())
                .name(headquarters.getName())
                .address(headquarters.getAddress())
                .phone(headquarters.getPhone())
                .email(headquarters.getEmail())
                .build();
    }
}
