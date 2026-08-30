package com.hopestar.hfms.common.util;

import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.common.exception.BusinessValidationException;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Centralised money arithmetic. All financial entities in the approved
 * database design store amounts as {@code DECIMAL(12,2)}; every module
 * (Student Payments, Salaries, Expenses, Loans, Ledger) must round and
 * compare amounts the same way, so that logic lives here once rather than
 * being repeated — and potentially drifting — across modules.
 */
@UtilityClass
public class MoneyUtil {

    public final int SCALE = 2;
    public final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
    }

    public BigDecimal normalize(BigDecimal amount) {
        return amount == null ? zero() : amount.setScale(SCALE, ROUNDING);
    }

    public BigDecimal add(BigDecimal a, BigDecimal b) {
        return normalize(normalize(a).add(normalize(b)));
    }

    public BigDecimal subtract(BigDecimal a, BigDecimal b) {
        return normalize(normalize(a).subtract(normalize(b)));
    }

    /**
     * Multiplies at full precision and rounds only the final result to
     * {@link #SCALE}. Every caller uses this for {@code amount ×
     * exchangeRateToUsd}, where the rate is stored at {@code DECIMAL(14,6)}
     * precisely because it needs more than 2 decimal places (e.g. AFN rates
     * like {@code 0.015400}); pre-rounding either operand to {@link #SCALE}
     * before multiplying would silently destroy that precision.
     */
    public BigDecimal multiply(BigDecimal a, BigDecimal b) {
        BigDecimal left = a == null ? BigDecimal.ZERO : a;
        BigDecimal right = b == null ? BigDecimal.ZERO : b;
        return normalize(left.multiply(right));
    }

    public BigDecimal percentageOf(BigDecimal base, BigDecimal percentage) {
        return normalize(normalize(base)
                .multiply(normalize(percentage))
                .divide(BigDecimal.valueOf(100), 10, ROUNDING));
    }

    public boolean isNegative(BigDecimal amount) {
        return normalize(amount).compareTo(BigDecimal.ZERO) < 0;
    }

    public boolean isPositive(BigDecimal amount) {
        return normalize(amount).compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isZero(BigDecimal amount) {
        return normalize(amount).compareTo(BigDecimal.ZERO) == 0;
    }

    /**
     * The single implementation of the project-wide exchange-rate rule:
     * USD is always exactly {@code 1.000000}, regardless of what was
     * submitted; AFN/EUR require a positive, manually-entered rate. Every
     * module that records a currency-denominated financial amount
     * (Student Contracts, the Ledger, Student Payments, and — in later
     * phases — Salaries, Expenses, Loans, Advances) calls this instead of
     * each re-implementing the same check, per the project's "historical
     * exchange rates must never change, and are never trusted from a
     * client" rule.
     *
     * @throws BusinessValidationException if the currency is not USD and
     *         {@code requestedRate} is missing, zero, or negative
     */
    public BigDecimal resolveExchangeRateToUsd(SupportedCurrency currencyCode, BigDecimal requestedRate) {
        if (currencyCode == SupportedCurrency.USD) {
            return BigDecimal.ONE;
        }
        if (requestedRate == null || isNegative(requestedRate) || isZero(requestedRate)) {
            throw new BusinessValidationException(
                    "A positive exchange rate to USD is required for currency " + currencyCode + ".");
        }
        return requestedRate;
    }
}
