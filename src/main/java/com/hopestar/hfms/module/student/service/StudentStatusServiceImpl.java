package com.hopestar.hfms.module.student.service;

import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.module.student.dto.StudentStatusResponseDTO;
import com.hopestar.hfms.module.student.entity.StudentStatus;
import com.hopestar.hfms.module.student.repository.StudentStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentStatusServiceImpl implements StudentStatusService {

    private final StudentStatusRepository studentStatusRepository;

    @Override
    public List<StudentStatusResponseDTO> listActive() {
        return studentStatusRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public StudentStatusResponseDTO getById(Long id) {
        return studentStatusRepository.findById(id)
                .filter(StudentStatus::isActive)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Student status", id));
    }

    private StudentStatusResponseDTO toResponseDTO(StudentStatus status) {
        return StudentStatusResponseDTO.builder()
                .id(status.getId())
                .name(status.getName())
                .description(status.getDescription())
                .build();
    }
}
