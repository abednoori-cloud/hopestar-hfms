package com.hopestar.hfms.module.student.service;

import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.module.student.dto.ProgramResponseDTO;
import com.hopestar.hfms.module.student.entity.Program;
import com.hopestar.hfms.module.student.repository.ProgramRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgramServiceImpl implements ProgramService {

    private final ProgramRepository programRepository;

    @Override
    public List<ProgramResponseDTO> listActive() {
        return programRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public ProgramResponseDTO getById(Long id) {
        return programRepository.findById(id)
                .filter(Program::isActive)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Program", id));
    }

    private ProgramResponseDTO toResponseDTO(Program program) {
        return ProgramResponseDTO.builder()
                .id(program.getId())
                .name(program.getName())
                .destinationCountry(program.getDestinationCountry())
                .basePrice(program.getBasePrice())
                .currencyCode(program.getCurrencyCode())
                .description(program.getDescription())
                .active(program.isActive())
                .build();
    }
}
