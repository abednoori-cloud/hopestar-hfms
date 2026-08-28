package com.hopestar.hfms.module.finance.employee.service;

import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.module.finance.employee.dto.EmployeeStatusResponseDTO;
import com.hopestar.hfms.module.finance.employee.entity.EmployeeStatus;
import com.hopestar.hfms.module.finance.employee.repository.EmployeeStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployeeStatusServiceImpl implements EmployeeStatusService {

    private final EmployeeStatusRepository employeeStatusRepository;

    @Override
    public List<EmployeeStatusResponseDTO> listActive() {
        return employeeStatusRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public EmployeeStatusResponseDTO getById(Long id) {
        return employeeStatusRepository.findById(id)
                .filter(EmployeeStatus::isActive)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Employee status", id));
    }

    private EmployeeStatusResponseDTO toResponseDTO(EmployeeStatus status) {
        return EmployeeStatusResponseDTO.builder()
                .id(status.getId())
                .name(status.getName())
                .description(status.getDescription())
                .build();
    }
}
