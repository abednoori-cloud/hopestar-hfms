package com.hopestar.hfms.module.finance.ledger.repository;

import com.hopestar.hfms.module.finance.ledger.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByActiveTrueOrderByNameAsc();

    Optional<PaymentMethod> findByNameAndActiveTrue(String name);

    Optional<PaymentMethod> findByCodeAndActiveTrue(String code);

    boolean existsByName(String name);
}
