package com.hopestar.hfms.module.finance.studentpayment.dto;

import com.hopestar.hfms.common.enums.SupportedCurrency;
import com.hopestar.hfms.module.finance.studentpayment.entity.PaymentStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Optional multi-criteria filter for the payment list/search page. Every
 * field is optional; {@code StudentPaymentServiceImpl} composes only the
 * criteria actually supplied into a JPA {@code Specification} (see
 * {@code StudentPaymentSpecifications}), mirroring {@code
 * StudentSearchDTO}/{@code StudentSpecifications} in the Student module.
 */
@Getter
@Setter
@NoArgsConstructor
public class StudentPaymentSearchDTO {

    /** Matches against payment number (contains, case-insensitive). */
    private String paymentNumber;

    /** Matches against receipt number (contains, case-insensitive). */
    private String receiptNumber;

    /** Matches against the linked student's full name (contains, case-insensitive). */
    private String studentName;

    /** Matches against the linked student's code (contains, case-insensitive). */
    private String studentCode;

    private SupportedCurrency currencyCode;

    private Long paymentMethodId;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private PaymentStatus status;

    /** Page number, 0-based. */
    private int page = 0;

    /** Page size. */
    private int size = 20;
}
