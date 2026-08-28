package com.hopestar.hfms.module.finance.ledger.service;

import com.hopestar.hfms.module.finance.ledger.dto.PaymentMethodResponseDTO;

import java.util.List;

/**
 * Read-only access to the {@code payment_methods} lookup table for
 * dropdowns across every finance submodule (Student Payments, Salaries,
 * Expenses, etc.).
 */
public interface PaymentMethodService {

    List<PaymentMethodResponseDTO> listActive();

    PaymentMethodResponseDTO getById(Long id);
}
