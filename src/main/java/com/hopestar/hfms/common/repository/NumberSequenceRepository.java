package com.hopestar.hfms.common.repository;

import com.hopestar.hfms.common.entity.NumberSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NumberSequenceRepository extends JpaRepository<NumberSequence, Long> {

    /**
     * Locks the matching sequence row ({@code SELECT ... FOR UPDATE}) so
     * concurrent callers serialize on this row rather than racing to
     * increment {@code lastValue}, per the approved Invoice Numbering
     * Strategy (§6). Must only be called from within an active
     * transaction (see {@code SequenceGeneratorServiceImpl}).
     *
     * @param sequenceYear the year to scope by, or {@code 0} for
     *                      sequences that are not year-scoped
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM NumberSequence s WHERE s.sequenceKey = :sequenceKey "
            + "AND s.sequenceYear = :sequenceYear AND s.branchId = :branchId")
    Optional<NumberSequence> lockSequence(@Param("sequenceKey") String sequenceKey,
                                           @Param("sequenceYear") int sequenceYear,
                                           @Param("branchId") long branchId);
}
