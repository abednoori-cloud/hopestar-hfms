package com.hopestar.hfms.module.finance.studentpayment.repository;

import com.hopestar.hfms.module.finance.studentpayment.entity.PaymentStatus;
import com.hopestar.hfms.module.finance.studentpayment.entity.StudentPayment;
import com.hopestar.hfms.module.student.entity.ContractStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * {@link JpaSpecificationExecutor} backs {@code StudentPaymentService}'s
 * multi-criteria search (payment/receipt number, student name/code,
 * currency, payment method, date range, status), mirroring {@code
 * StudentRepository}/{@code TransactionRepository} in the other modules.
 */
public interface StudentPaymentRepository extends JpaRepository<StudentPayment, Long>,
        JpaSpecificationExecutor<StudentPayment> {

    Optional<StudentPayment> findByIdAndActiveTrue(Long id);

    Optional<StudentPayment> findByPaymentNumberAndActiveTrue(String paymentNumber);

    Optional<StudentPayment> findByReceiptNumberAndActiveTrue(String receiptNumber);

    List<StudentPayment> findByStudentIdAndActiveTrueOrderByPaymentDateDesc(Long studentId);

    List<StudentPayment> findByContractIdAndActiveTrueOrderByPaymentDateDesc(Long contractId);

    boolean existsByPaymentNumber(String paymentNumber);

    boolean existsByReceiptNumber(String receiptNumber);

    /**
     * Sums the USD-equivalent amount of every {@code POSTED} payment
     * against a contract -- per the approved business rules, only
     * {@code POSTED} payments affect a contract's remaining balance;
     * {@code DRAFT}, {@code CANCELLED}, and {@code REFUNDED} payments
     * must not. Used by {@code StudentContractServiceImpl} (the single
     * place that computes remaining balance) and nowhere else, so this
     * calculation is never duplicated.
     */
    @Query("SELECT COALESCE(SUM(p.usdEquivalentAmount), 0) FROM StudentPayment p "
            + "WHERE p.contract.id = :contractId AND p.status = :status AND p.active = true")
    BigDecimal sumUsdEquivalentAmountByContractIdAndStatus(@Param("contractId") Long contractId,
                                                            @Param("status") PaymentStatus status);

    /**
     * Sums the USD-equivalent amount of every payment in the given status
     * whose contract is in the given status -- used by {@code
     * DashboardServiceImpl} together with {@code StudentContractRepository
     * .sumUsdEquivalentAmountByStatusAndActiveTrue} to compute total
     * outstanding receivables as {@code (total active contract value) -
     * (total posted payments against active contracts)}, two independent
     * flat sums rather than a join (which would double-count a contract
     * with more than one posted payment).
     */
    @Query("SELECT COALESCE(SUM(p.usdEquivalentAmount), 0) FROM StudentPayment p "
            + "WHERE p.status = :status AND p.active = true "
            + "AND p.contract.status = :contractStatus AND p.contract.active = true")
    BigDecimal sumUsdEquivalentAmountByStatusAndContractStatus(@Param("status") PaymentStatus status,
                                                                @Param("contractStatus") ContractStatus contractStatus);
}
