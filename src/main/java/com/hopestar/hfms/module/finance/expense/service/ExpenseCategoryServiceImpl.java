package com.hopestar.hfms.module.finance.expense.service;

import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.module.finance.expense.dto.ExpenseCategoryResponseDTO;
import com.hopestar.hfms.module.finance.expense.entity.ExpenseCategory;
import com.hopestar.hfms.module.finance.expense.repository.ExpenseCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExpenseCategoryServiceImpl implements ExpenseCategoryService {

    private final ExpenseCategoryRepository expenseCategoryRepository;

    @Override
    public List<ExpenseCategoryResponseDTO> listActive() {
        return expenseCategoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public ExpenseCategoryResponseDTO getById(Long id) {
        return expenseCategoryRepository.findById(id)
                .filter(ExpenseCategory::isActive)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Expense category", id));
    }

    private ExpenseCategoryResponseDTO toResponseDTO(ExpenseCategory category) {
        return ExpenseCategoryResponseDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .build();
    }
}
