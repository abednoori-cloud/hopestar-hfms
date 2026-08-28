package com.hopestar.hfms.common.service;

import com.hopestar.hfms.common.entity.NumberSequence;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import com.hopestar.hfms.common.repository.NumberSequenceRepository;
import com.hopestar.hfms.common.util.StringUtil;
import com.hopestar.hfms.module.auth.entity.Branch;
import com.hopestar.hfms.module.auth.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;

/**
 * Implements {@link SequenceGeneratorService} using a pessimistically
 * locked {@code number_sequences} row per counter, per the approved
 * Invoice Numbering Strategy (Section 6).
 * <p>
 * Runs in its own {@code REQUIRES_NEW} transaction so a generated number
 * is never reused even if the caller's outer transaction (e.g. saving the
 * new Student) subsequently rolls back -- matching the business rule that
 * business-key sequences are never reused, only ever skipped.
 * <p>
 * {@code sequence_year} and {@code branch_id} are stored as non-null
 * sentinel-backed columns rather than nullable ones (MySQL treats NULL as
 * distinct in unique indexes, which would otherwise defeat the
 * uniqueness guarantee on {@code number_sequences}) -- see this class's
 * companion migration, {@code V2__number_sequences.sql}.
 */
@Service
@RequiredArgsConstructor
public class SequenceGeneratorServiceImpl implements SequenceGeneratorService {

    /** Sentinel meaning "this sequence is not scoped to a calendar year". */
    private static final int NOT_YEAR_SCOPED = 0;

    private final NumberSequenceRepository numberSequenceRepository;
    private final BranchRepository branchRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextYearlyValue(String sequenceKey, String prefix, int paddingLength, Long branchId) {
        int currentYear = Year.now().getValue();
        long resolvedBranchId = resolveBranchId(branchId);

        NumberSequence sequence = numberSequenceRepository
                .lockSequence(sequenceKey, currentYear, resolvedBranchId)
                .orElseGet(() -> createSequence(sequenceKey, prefix, paddingLength, currentYear, resolvedBranchId));

        sequence.setLastValue(sequence.getLastValue() + 1);
        numberSequenceRepository.save(sequence);

        return format(sequence, String.valueOf(currentYear));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String nextPerpetualValue(String sequenceKey, String prefix, int paddingLength, Long branchId) {
        long resolvedBranchId = resolveBranchId(branchId);

        NumberSequence sequence = numberSequenceRepository
                .lockSequence(sequenceKey, NOT_YEAR_SCOPED, resolvedBranchId)
                .orElseGet(() -> createSequence(sequenceKey, prefix, paddingLength, NOT_YEAR_SCOPED, resolvedBranchId));

        sequence.setLastValue(sequence.getLastValue() + 1);
        numberSequenceRepository.save(sequence);

        return format(sequence, null);
    }

    /**
     * Resolves the branch to scope a counter to. A caller may pass a
     * specific branch id explicitly (once Multi-branch Support is
     * enabled); until then, every call defaults to the single seeded
     * headquarters branch from {@code V1__init.sql}.
     */
    private long resolveBranchId(Long requestedBranchId) {
        if (requestedBranchId != null) {
            return requestedBranchId;
        }
        Branch headquarters = branchRepository.findFirstByHeadquartersTrueAndActiveTrue()
                .orElseThrow(() -> new BusinessValidationException(
                        "No headquarters branch is configured; cannot generate a sequence number."));
        return headquarters.getId();
    }

    private NumberSequence createSequence(String sequenceKey, String prefix, int paddingLength,
                                           int year, long branchId) {
        NumberSequence sequence = NumberSequence.builder()
                .sequenceKey(sequenceKey)
                .sequenceYear(year)
                .branchId(branchId)
                .prefix(prefix)
                .paddingLength(paddingLength)
                .lastValue(0L)
                .build();
        // Persist immediately so the pessimistic lock has a row to guard
        // on the very next call; flush to surface any race (a duplicate
        // key on uk_number_sequences_key_year_branch) immediately.
        return numberSequenceRepository.saveAndFlush(sequence);
    }

    private String format(NumberSequence sequence, String yearSegment) {
        String paddedValue = StringUtil.padSequence(sequence.getLastValue(), sequence.getPaddingLength());
        return yearSegment == null
                ? "%s-%s".formatted(sequence.getPrefix(), paddedValue)
                : "%s-%s-%s".formatted(sequence.getPrefix(), yearSegment, paddedValue);
    }
}
