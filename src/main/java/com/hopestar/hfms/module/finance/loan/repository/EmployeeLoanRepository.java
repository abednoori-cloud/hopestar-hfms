package com.hopestar.hfms.module.finance.loan.repository;

import com.hopestar.hfms.module.finance.loan.entity.EmployeeLoan;
import com.hopestar.hfms.module.finance.loan.entity.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * {@link JpaSpecificationExecutor} backs {@code EmployeeLoanService}'s
 * multi-criteria search (employee name/code, status), mirroring {@code
 * SalaryRepository}/{@code EmployeeRepository} in the other Finance
 * submodules.
 */
public interface EmployeeLoanRepository extends JpaRepository<EmployeeLoan, Long>, JpaSpecificationExecutor<EmployeeLoan> {

    Optional<EmployeeLoan> findByIdAndActiveTrue(Long id);

    Optional<EmployeeLoan> findByLoanNumberAndActiveTrue(String loanNumber);

    List<EmployeeLoan> findByEmployeeIdAndActiveTrueOrderByLoanDateDesc(Long employeeId);

    boolean existsByLoanNumber(String loanNumber);

    /** Count of loans in the given status, for the Dashboard's "active loans" figure. */
    long countByStatusAndActiveTrue(LoanStatus status);

    /**
     * Sums the USD-equivalent remaining balance of every loan in the given
     * status, for the Dashboard's "loans outstanding" figure. {@code
     * remainingBalance} is stored in the loan's own {@link
     * EmployeeLoan#getCurrency()}, not USD (it starts equal to {@code
     * loanAmount} and is decremented by each posted repayment in that same
     * currency) -- so, exactly like {@code usdEquivalentAmount} is derived
     * from {@code loanAmount * exchangeRateToUsd} at create/update time,
     * this multiplies the *remaining* balance by that same stored {@code
     * exchangeRateToUsd} rather than re-deriving or looking up a rate.
     */
    @Query("SELECT COALESCE(SUM(l.remainingBalance * l.exchangeRateToUsd), 0) FROM EmployeeLoan l "
            + "WHERE l.status = :status AND l.active = true")
    BigDecimal sumRemainingBalanceUsdByStatusAndActiveTrue(@Param("status") LoanStatus status);
}
