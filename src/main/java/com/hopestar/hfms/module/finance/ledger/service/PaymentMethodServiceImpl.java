package com.hopestar.hfms.module.finance.ledger.service;

import com.hopestar.hfms.common.exception.ResourceNotFoundException;
import com.hopestar.hfms.module.finance.ledger.dto.PaymentMethodResponseDTO;
import com.hopestar.hfms.module.finance.ledger.entity.PaymentMethod;
import com.hopestar.hfms.module.finance.ledger.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    @Override
    public List<PaymentMethodResponseDTO> listActive() {
        return paymentMethodRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    public PaymentMethodResponseDTO getById(Long id) {
        return paymentMethodRepository.findById(id)
                .filter(PaymentMethod::isActive)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method", id));
    }

    private PaymentMethodResponseDTO toResponseDTO(PaymentMethod paymentMethod) {
        return PaymentMethodResponseDTO.builder()
                .id(paymentMethod.getId())
                .name(paymentMethod.getName())
                .code(paymentMethod.getCode())
                .description(paymentMethod.getDescription())
                .build();
    }
}
