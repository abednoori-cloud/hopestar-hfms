package com.hopestar.hfms.common.enums;

/**
 * ISO 4217 currency codes the system currently accepts for
 * currency-denominated amounts (program pricing, contract amounts).
 * Deliberately a small, explicit enum rather than a full {@code currencies}
 * master table for now — the full {@code Currency} entity (with exchange
 * rates to a base currency) belongs to the Finance ledger module per the
 * approved architecture (§2.4) and will supersede this enum's role once
 * that module lands; the enum values below are chosen to match what that
 * table will seed, so no data conversion will be needed later.
 */
public enum SupportedCurrency {
    USD,
    EUR,
    AFN
}
