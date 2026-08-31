package com.hopestar.hfms.common.util;

import lombok.experimental.UtilityClass;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Shared date/time helpers used across modules — e.g. the Finance module's
 * salary period keys, the Report module's date-range filters, and the
 * yearly reset of student/receipt number sequences (approved architecture
 * §6).
 */
@UtilityClass
public class DateUtil {

    public final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    public final DateTimeFormatter DISPLAY_DATETIME = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
    public final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    public String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DISPLAY_DATE);
    }

    public String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? "" : dateTime.format(DISPLAY_DATETIME);
    }

    public int currentYear() {
        return LocalDate.now().getYear();
    }

    public YearMonth currentPeriod() {
        return YearMonth.now();
    }

    public LocalDate firstDayOfMonth(LocalDate date) {
        return date.withDayOfMonth(1);
    }

    public LocalDate lastDayOfMonth(LocalDate date) {
        return date.withDayOfMonth(date.lengthOfMonth());
    }

    public LocalDate firstDayOfYear(LocalDate date) {
        return date.withDayOfYear(1);
    }

    public LocalDate lastDayOfYear(LocalDate date) {
        return date.withDayOfYear(date.lengthOfYear());
    }

    public boolean isWithinDays(LocalDate date, int days) {
        if (date == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return !date.isBefore(today) && !date.isAfter(today.plusDays(days));
    }
}
