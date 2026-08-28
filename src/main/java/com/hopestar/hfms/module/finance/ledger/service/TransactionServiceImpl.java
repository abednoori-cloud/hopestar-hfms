package com.hopestar.hfms.module.finance.ledger.service;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.module.finance.ledger.dto.TransactionResponseDTO;
import com.hopestar.hfms.module.finance.ledger.dto.TransactionSearchDTO;
import com.hopestar.hfms.module.finance.ledger.entity.Transaction;
import com.hopestar.hfms.module.finance.ledger.repository.TransactionRepository;
import com.hopestar.hfms.module.finance.ledger.repository.TransactionSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionServiceImpl implements TransactionService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final TransactionRepository transactionRepository;

    @Override
    public PageResponse<TransactionResponseDTO> search(TransactionSearchDTO searchDTO) {
        int page = Math.max(searchDTO.getPage(), 0);
        int size = searchDTO.getSize() <= 0 ? DEFAULT_PAGE_SIZE : Math.min(searchDTO.getSize(), MAX_PAGE_SIZE);

        Page<Transaction> result = transactionRepository.findAll(
                TransactionSpecifications.fromSearchCriteria(searchDTO),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate")));

        return PageResponse.from(result.map(TransactionMapper::toResponseDTO));
    }

    @Override
    public List<TransactionResponseDTO> findByReference(String referenceTable, Long referenceId) {
        return transactionRepository
                .findByReferenceTableAndReferenceIdAndActiveTrueOrderByTransactionDateDesc(referenceTable, referenceId)
                .stream()
                .map(TransactionMapper::toResponseDTO)
                .toList();
    }
}
