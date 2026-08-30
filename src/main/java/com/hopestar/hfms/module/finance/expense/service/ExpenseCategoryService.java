package com.hopestar.hfms.module.finance.expense.service;

import com.hopestar.hfms.module.finance.expense.dto.ExpenseCategoryResponseDTO;

import java.util.List;

/** Read-only access to the {@code expense_categories} lookup list, mirroring {@code PaymentMethodService}. */
public interface ExpenseCategoryService {

    List<ExpenseCategoryResponseDTO> listActive();

    ExpenseCategoryResponseDTO getById(Long id);
}
