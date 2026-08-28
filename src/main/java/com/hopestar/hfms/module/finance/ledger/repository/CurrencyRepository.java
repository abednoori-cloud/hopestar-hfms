package com.hopestar.hfms.module.finance.ledger.repository;

import com.hopestar.hfms.module.finance.ledger.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    List<Currency> findByActiveTrueOrderByCodeAsc();

    Optional<Currency> findByCodeAndActiveTrue(String code);

    Optional<Currency> findFirstByBaseCurrencyTrueAndActiveTrue();

    boolean existsByCode(String code);
}
