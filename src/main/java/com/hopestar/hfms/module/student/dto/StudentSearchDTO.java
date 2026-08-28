package com.hopestar.hfms.module.student.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Optional multi-criteria filter for the student list/search page. Every
 * field is optional; {@code StudentServiceImpl} composes only the
 * criteria that are actually supplied into a JPA {@code Specification}
 * (see {@code StudentSpecifications}), so any subset — or none — of
 * these fields may be set.
 */
@Getter
@Setter
@NoArgsConstructor
public class StudentSearchDTO {

    /** Matches against full name (contains, case-insensitive). */
    private String name;

    /** Matches against student code (contains, case-insensitive). */
    private String studentCode;

    /** Matches against passport number (exact, case-insensitive). */
    private String passportNumber;

    /** Matches against phone (contains). */
    private String phone;

    /** Matches against email (contains, case-insensitive). */
    private String email;

    private Long programId;

    private String destinationCountry;

    private Long statusId;

    /** Page number, 0-based. */
    private int page = 0;

    /** Page size. */
    private int size = 20;
}
