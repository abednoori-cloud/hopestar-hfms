package com.hopestar.hfms.module.finance.ledger.service;

import com.hopestar.hfms.common.dto.PageResponse;
import com.hopestar.hfms.module.finance.ledger.dto.TransactionResponseDTO;
import com.hopestar.hfms.module.finance.ledger.dto.TransactionSearchDTO;

import java.util.List;

/**
 * Read-only querying of the ledger for the Report and Dashboard modules
 * (later phases) — filtering by type, direction, status, date range,
 * reference, and branch. Deliberately has no write operations: creating
 * or voiding a transaction is exclusively {@link LedgerService}'s
 * responsibility, per the approved Finance module architecture.
 */
public interface TransactionService {

    PageResponse<TransactionResponseDTO> search(TransactionSearchDTO searchDTO);

    /** All (non-voided or otherwise) transactions linked to a given originating record. */
    List<TransactionResponseDTO> findByReference(String referenceTable, Long referenceId);
}
