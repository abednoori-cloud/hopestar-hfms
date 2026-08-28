-- =====================================================================
-- V4__student_contracts_currency_conversion.sql
-- Adds manual-exchange-rate tracking to student_contracts, per the
-- project-wide currency rule:
--   * Base currency = USD.
--   * Supported currencies = USD, EUR, AFN.
--   * Exchange rates are entered manually by the user (no live FX feed).
--   * Every financial transaction stores: original amount (already
--     present as final_amount/total_contract_amount), original currency
--     (currency_code, already present), exchange rate, and converted USD
--     amount.
-- Reports and the Dashboard (later phases) will read usd_equivalent_amount;
-- final_amount + currency_code remain available for original-currency
-- display, per that same requirement.
-- =====================================================================

ALTER TABLE student_contracts
    ADD COLUMN exchange_rate_to_usd  DECIMAL(14,6) NOT NULL DEFAULT 1.000000 AFTER currency_code,
    ADD COLUMN usd_equivalent_amount DECIMAL(12,2)  NOT NULL DEFAULT 0.00     AFTER exchange_rate_to_usd;

-- Backfill: every contract inserted so far by V3's seed data path (none
-- shipped with actual contract rows) would otherwise be inconsistent;
-- for any existing row, treat the recorded amount as already being in
-- USD (rate = 1) so usd_equivalent_amount matches final_amount exactly.
UPDATE student_contracts
SET usd_equivalent_amount = final_amount
WHERE usd_equivalent_amount = 0.00;

ALTER TABLE student_contracts
    ADD CONSTRAINT chk_student_contracts_exchange_rate_positive CHECK (exchange_rate_to_usd > 0),
    ADD CONSTRAINT chk_student_contracts_usd_equivalent_non_negative CHECK (usd_equivalent_amount >= 0);
